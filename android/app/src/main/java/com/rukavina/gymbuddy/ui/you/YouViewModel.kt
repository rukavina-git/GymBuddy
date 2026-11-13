package com.rukavina.gymbuddy.ui.you

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.rukavina.gymbuddy.data.export.DataExporter
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.data.sync.AccountDeletionCoordinator
import com.rukavina.gymbuddy.data.sync.AccountDeletionResult
import com.rukavina.gymbuddy.data.sync.GuestMerge
import com.rukavina.gymbuddy.data.sync.LocalDataCounts
import com.rukavina.gymbuddy.data.sync.LogoutCoordinator
import com.rukavina.gymbuddy.data.sync.LogoutResult
import com.rukavina.gymbuddy.data.sync.SyncLocalStore
import com.rukavina.gymbuddy.domain.sync.SyncReason
import com.rukavina.gymbuddy.data.sync.SyncScheduler
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository
import com.rukavina.gymbuddy.utils.validation.isStrongPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Clock
import javax.inject.Inject

/**
 * You tab (7.10a, R15-1/2): account card with the backup line, the
 * settings rows, sign out, delete account, guest erase and export.
 */
@HiltViewModel
class YouViewModel @Inject constructor(
    private val repository: UserProfileRepository,
    private val logoutCoordinator: LogoutCoordinator,
    private val accountDeletionCoordinator: AccountDeletionCoordinator,
    @ApplicationContext private val appContext: Context,
    private val guestMerge: GuestMerge,
    private val preferences: AppPreferencesRepository,
    private val dataExporter: DataExporter,
    exercises: ExerciseRepository,
    templates: WorkoutTemplateRepository,
    store: SyncLocalStore,
    private val scheduler: SyncScheduler,
    private val onlineCheck: OnlineCheck,
    private val clock: Clock,
) : ViewModel() {

    private val auth = MutableStateFlow(readAuth())

    private val profile = repository.observeProfile().catch { emit(null) }

    private val backup = combine(
        store.observeOutbox().catch { emit(emptyList()) },
        scheduler.status,
    ) { outbox, status ->
        backupLine(outbox.size, outbox.minOfOrNull { it.enqueuedAt }, status.lastSyncAt, clock.millis())
    }

    private val prefs = combine(preferences.preferredUnits, preferences.restBuzz, ::Pair)

    private val hidden = combine(
        exercises.getHiddenExercises().map { it.size }.catch { emit(0) },
        templates.getHiddenTemplates().map { it.size }.catch { emit(0) },
        ::Pair,
    )

    val uiState: StateFlow<YouUiState> = combine(auth, profile, backup, prefs, hidden) { a, p, b, (units, buzz), (he, ht) ->
        YouUiState(
            userName = p?.name.orEmpty(),
            userEmail = p?.email?.takeIf { it.isNotBlank() } ?: a.email,
            avatarId = p?.avatarId,
            isLoading = false,
            hasPassword = a.hasPassword,
            isGoogleOnly = a.hasGoogle && !a.hasPassword,
            isGuest = repository.isGuest,
            units = units,
            restBuzz = buzz,
            hiddenExercises = he,
            hiddenTemplates = ht,
            backup = b,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YouUiState())

    private val _logoutEvent = MutableSharedFlow<Unit>()
    /** Signed out, erased or deleted: back to Welcome. */
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    private data class AuthInfo(val email: String, val hasPassword: Boolean, val hasGoogle: Boolean)

    private fun readAuth(): AuthInfo {
        val user = FirebaseAuth.getInstance().currentUser
        val providers = user?.providerData?.map { it.providerId }.orEmpty()
        return AuthInfo(
            email = user?.email.orEmpty(),
            hasPassword = EmailAuthProvider.PROVIDER_ID in providers,
            hasGoogle = GoogleAuthProvider.PROVIDER_ID in providers,
        )
    }

    fun setUnits(units: PreferredUnits) {
        viewModelScope.launch { preferences.setPreferredUnits(units) }
    }

    fun setRestBuzz(on: Boolean) {
        viewModelScope.launch { preferences.setRestBuzz(on) }
    }

    /** "Couldn't back up · Retry". */
    fun retryBackup() {
        scheduler.requestSync(SyncReason.MANUAL)
    }

    // ---- counts (export sheet, erase and delete dialogs) ----

    private val _counts = MutableStateFlow<LocalDataCounts?>(null)
    /** What's on this phone; null until loaded. */
    val counts: StateFlow<LocalDataCounts?> = _counts

    /** Call when a sheet or dialog that shows the counts opens. */
    fun loadCounts() {
        _counts.value = null
        viewModelScope.launch { _counts.value = guestMerge.openCounts() }
    }

    // ---- export (ADR 0015) ----

    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState

    /** Writes the export JSON to [uri], the file the user picked in the Save dialog. */
    fun export(uri: Uri) {
        if (_exportState.value == ExportUiState.Saving) return
        _exportState.value = ExportUiState.Saving
        viewModelScope.launch {
            _exportState.value = try {
                withContext(Dispatchers.IO) {
                    val out = appContext.contentResolver.openOutputStream(uri, "wt")
                        ?: throw IOException("can't open $uri")
                    out.use { dataExporter.export(it) }
                }
                ExportUiState.Done
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "export failed", e)
                ExportUiState.Failed
            }
        }
    }

    /** Call once the result was shown. */
    fun clearExportState() {
        _exportState.value = ExportUiState.Idle
    }

    // ---- guest erase (R15-10) ----

    /** The logout wipe on the guest's file. Ends like a logout, on Welcome. */
    fun eraseGuestData() {
        viewModelScope.launch {
            try {
                guestMerge.eraseGuest()
                preferences.setGuest(false)
                _logoutEvent.emit(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "erase failed", e)
            }
        }
    }

    // ---- sign out (R15-8, R15-13) ----

    private val _logoutState = MutableStateFlow<LogoutUiState>(LogoutUiState.Idle)
    val logoutState: StateFlow<LogoutUiState> = _logoutState

    /**
     * The Sign out row. Nothing waiting: sign out (the flush still runs).
     * Changes only on this phone: ask first ([LogoutUiState.Confirm]).
     */
    fun signOut() {
        if (_logoutState.value != LogoutUiState.Idle) return
        viewModelScope.launch {
            val lines = try {
                unsyncedChangeLines(accountDeletionCoordinator.unsyncedChanges())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "pending count failed", e)
                emptyList()
            }
            if (lines.isEmpty()) logout() else _logoutState.value = LogoutUiState.Confirm(lines, offline = !onlineCheck.isOnline())
        }
    }

    /**
     * "Back up and sign out": pushes first. If the push fails, nothing
     * happens until the user picks "Sign out anyway".
     */
    fun logout() {
        if (_logoutState.value == LogoutUiState.Flushing) return
        _logoutState.value = LogoutUiState.Flushing
        viewModelScope.launch {
            try {
                when (val result = logoutCoordinator.requestLogout()) {
                    LogoutResult.LoggedOut -> finishLogout()
                    is LogoutResult.UnsyncedChanges ->
                        _logoutState.value = LogoutUiState.UnsyncedWarning(unsyncedChangeLines(result.pendingByType))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "logout failed", e)
                _logoutState.value = LogoutUiState.Failed(LOGOUT_FAILED_MESSAGE, afterWarning = false)
            }
        }
    }

    /** "Sign out anyway": discard the unsynced changes and sign out. */
    fun confirmLogoutAndDelete() {
        if (_logoutState.value == LogoutUiState.Flushing) return
        _logoutState.value = LogoutUiState.Flushing
        viewModelScope.launch {
            try {
                logoutCoordinator.logOutAndDelete()
                finishLogout()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "log out and delete failed", e)
                _logoutState.value = LogoutUiState.Failed(LOGOUT_FAILED_MESSAGE, afterWarning = true)
            }
        }
    }

    /** Retry from [LogoutUiState.Failed]: repeats whichever step failed. */
    fun retryLogout() {
        val failed = _logoutState.value as? LogoutUiState.Failed ?: return
        _logoutState.value = LogoutUiState.Idle
        if (failed.afterWarning) confirmLogoutAndDelete() else logout()
    }

    fun cancelLogout() {
        _logoutState.value = LogoutUiState.Idle
    }

    private suspend fun finishLogout() {
        _logoutState.value = LogoutUiState.Idle
        _logoutEvent.emit(Unit)
    }

    // ---- password ----

    /** Runs [block] with the signed-in user and their email, or reports why it can't. */
    private fun withEmailUser(onResult: (Boolean, String?) -> Unit, block: (FirebaseUser, String) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email
        when {
            user == null -> onResult(false, NOT_SIGNED_IN)
            email.isNullOrEmpty() -> onResult(false, NO_EMAIL)
            else -> block(user, email)
        }
    }

    /** Set up a password on a Google-only account. */
    fun setupPassword(password: String, onResult: (Boolean, String?) -> Unit) = withEmailUser(onResult) { user, email ->
        user.linkWithCredential(EmailAuthProvider.getCredential(email, password))
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.value = readAuth()
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Couldn't set the password")
                }
            }
    }

    /** Change the password of an email/password account. */
    fun changePassword(currentPassword: String, newPassword: String, onResult: (Boolean, String?) -> Unit) = withEmailUser(onResult) { user, email ->
        user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword))
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) onResult(true, null)
                        else onResult(false, updateTask.exception?.localizedMessage ?: "Couldn't change the password")
                    }
                } else {
                    onResult(false, "Current password is incorrect")
                }
            }
    }

    fun isPasswordStrong(password: String): Boolean = password.isStrongPassword()

    // ---- delete account (R15-9) ----

    private val _accountDeletionState = MutableStateFlow<AccountDeletionUiState>(AccountDeletionUiState.Idle)
    val accountDeletionState: StateFlow<AccountDeletionUiState> = _accountDeletionState

    private val _deletionUnsyncedLines = MutableStateFlow<List<String>>(emptyList())
    /** Unsynced changes the delete will discard. Empty when there are none. */
    val deletionUnsyncedLines: StateFlow<List<String>> = _deletionUnsyncedLines

    /** Call when the delete confirmation opens. */
    fun loadDeletionWarning() {
        loadCounts()
        viewModelScope.launch {
            _deletionUnsyncedLines.value = unsyncedChangeLines(accountDeletionCoordinator.unsyncedChanges())
        }
    }

    /**
     * Runs after a successful re-auth. Deletes on the server, then wipes the
     * device. Every failure ends in [AccountDeletionUiState.Failed].
     */
    fun deleteAccount() {
        if (_accountDeletionState.value == AccountDeletionUiState.InProgress) return
        _accountDeletionState.value = AccountDeletionUiState.InProgress
        viewModelScope.launch {
            try {
                when (accountDeletionCoordinator.deleteAccount()) {
                    AccountDeletionResult.Deleted -> {
                        _accountDeletionState.value = AccountDeletionUiState.Idle
                        _logoutEvent.emit(Unit)
                    }
                    AccountDeletionResult.Retryable ->
                        _accountDeletionState.value = AccountDeletionUiState.Failed(DELETE_OFFLINE_MESSAGE, canRetry = true)
                    AccountDeletionResult.SignInAgain ->
                        _accountDeletionState.value = AccountDeletionUiState.Failed(DELETE_REAUTH_MESSAGE, canRetry = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "account deletion failed", e)
                _accountDeletionState.value = AccountDeletionUiState.Failed(DELETE_FAILED_MESSAGE, canRetry = true)
            }
        }
    }

    fun retryAccountDeletion() {
        val failed = _accountDeletionState.value as? AccountDeletionUiState.Failed ?: return
        if (failed.canRetry) {
            _accountDeletionState.value = AccountDeletionUiState.Idle
            deleteAccount()
        }
    }

    fun dismissAccountDeletion() {
        _accountDeletionState.value = AccountDeletionUiState.Idle
    }

    /** Re-auth with the password, then delete. */
    fun deleteAccountWithPassword(password: String, onResult: (Boolean, String?) -> Unit) = withEmailUser(onResult) { user, email ->
        user.reauthenticate(EmailAuthProvider.getCredential(email, password))
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    onResult(true, null)
                    deleteAccount()
                } else {
                    onResult(false, "Incorrect password")
                }
            }
    }

    /** Re-auth with Google, then delete. */
    fun deleteAccountWithGoogle(context: Context, webClientId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val idToken = getGoogleIdToken(context, webClientId)
                    ?: return@launch onResult(false, "Couldn't get your Google sign-in")
                val user = FirebaseAuth.getInstance().currentUser
                    ?: return@launch onResult(false, NOT_SIGNED_IN)
                user.reauthenticate(GoogleAuthProvider.getCredential(idToken, null))
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            onResult(true, null)
                            deleteAccount()
                        } else {
                            onResult(false, authTask.exception?.localizedMessage ?: "Google sign-in failed")
                        }
                    }
            } catch (e: GetCredentialCancellationException) {
                onResult(false, null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Google sign-in failed")
            }
        }
    }

    private suspend fun getGoogleIdToken(context: Context, webClientId: String): String? {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(webClientId)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = CredentialManager.create(context).getCredential(context, request)
        return GoogleIdTokenCredential.createFrom(result.credential.data).idToken
    }

    private companion object {
        const val TAG = "YouViewModel"
        const val NOT_SIGNED_IN = "Not signed in"
        const val NO_EMAIL = "No email on this account"
        const val LOGOUT_FAILED_MESSAGE = "Couldn't sign out. Try again."
        const val DELETE_OFFLINE_MESSAGE = "Couldn't reach the server. Nothing was deleted. Check your connection and try again."
        const val DELETE_REAUTH_MESSAGE = "Your sign-in has expired. Close this and confirm it's you again."
        const val DELETE_FAILED_MESSAGE = "Couldn't delete your account. Try again."
    }
}
