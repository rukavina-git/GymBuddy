package com.rukavina.gymbuddy.ui.settings

import android.content.Context
import android.net.Uri
import com.rukavina.gymbuddy.data.export.DataExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
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
import com.google.firebase.auth.GoogleAuthProvider
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.data.sync.GuestMerge
import com.rukavina.gymbuddy.data.sync.LocalDataCounts
import com.rukavina.gymbuddy.data.sync.AccountDeletionCoordinator
import com.rukavina.gymbuddy.data.sync.AccountDeletionResult
import com.rukavina.gymbuddy.data.sync.LogoutCoordinator
import com.rukavina.gymbuddy.data.sync.LogoutResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import com.rukavina.gymbuddy.utils.validation.isStrongPassword
import javax.inject.Inject

data class SettingsUiState(
    val userName: String = "",
    val userEmail: String = "",
    val isLoading: Boolean = true,
    val hasPassword: Boolean = false,
    val isGoogleOnly: Boolean = false,
    val hasGoogle: Boolean = false,
    /** No account (ADR 0014): no sign out, password or delete account; erase instead. */
    val isGuest: Boolean = false
)

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data object Saving : ExportUiState
    data object Done : ExportUiState
    data object Failed : ExportUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: UserProfileRepository,
    private val logoutCoordinator: LogoutCoordinator,
    private val accountDeletionCoordinator: AccountDeletionCoordinator,
    @ApplicationContext private val appContext: Context,
    private val guestMerge: GuestMerge,
    private val preferences: AppPreferencesRepository,
    private val dataExporter: DataExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    // Read per call: the owner can change while this ViewModel lives.
    private val uid: String?
        get() = repository.ownerUid

    init {
        loadProfile()
    }

    fun refreshProfile() {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            val firebaseEmail = user?.email ?: ""

            // Check auth providers
            val providers = user?.providerData?.map { it.providerId } ?: emptyList()
            val hasPassword = providers.contains(EmailAuthProvider.PROVIDER_ID)
            val hasGoogle = providers.contains(GoogleAuthProvider.PROVIDER_ID)
            val isGoogleOnly = hasGoogle && !hasPassword
            val isGuest = repository.isGuest

            uid?.let { userId ->
                val profile = repository.getProfile(userId)
                _uiState.value = if (profile != null) {
                    SettingsUiState(
                        userName = profile.name,
                        userEmail = profile.email.ifBlank { firebaseEmail },
                        isLoading = false,
                        hasPassword = hasPassword,
                        isGoogleOnly = isGoogleOnly,
                        hasGoogle = hasGoogle,
                        isGuest = isGuest
                    )
                } else {
                    SettingsUiState(
                        userName = "",
                        userEmail = firebaseEmail,
                        isLoading = false,
                        hasPassword = hasPassword,
                        isGoogleOnly = isGoogleOnly,
                        hasGoogle = hasGoogle,
                        isGuest = isGuest
                    )
                }
            } ?: run {
                _uiState.value = SettingsUiState(
                    userName = "",
                    userEmail = firebaseEmail,
                    isLoading = false,
                    hasPassword = hasPassword,
                    isGoogleOnly = isGoogleOnly,
                    hasGoogle = hasGoogle,
                    isGuest = isGuest
                )
            }
        }
    }

    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    /** Data export (Plan 6 step 07, ADR 0015). */
    val exportState: StateFlow<ExportUiState> = _exportState

    private val _exportCounts = MutableStateFlow<LocalDataCounts?>(null)
    /** What the export holds, for the sheet; null until loaded. */
    val exportCounts: StateFlow<LocalDataCounts?> = _exportCounts

    /** Call when the export sheet opens. */
    fun loadExportCounts() {
        _exportCounts.value = null
        viewModelScope.launch { _exportCounts.value = guestMerge.openCounts() }
    }

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

    private val _eraseCounts = MutableStateFlow<LocalDataCounts?>(null)
    /** The guest's counts for the erase confirmation; null until loaded. */
    val eraseCounts: StateFlow<LocalDataCounts?> = _eraseCounts

    /** Call when the guest's erase confirmation opens. */
    fun loadEraseCounts() {
        _eraseCounts.value = null
        viewModelScope.launch { _eraseCounts.value = guestMerge.openCounts() }
    }

    /**
     * "Erase data on this phone" for a guest (ADR 0014): the logout wipe on
     * the guest's file. Ends like a logout, on the Login screen.
     */
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

    private val _logoutState = MutableStateFlow<LogoutUiState>(LogoutUiState.Idle)
    val logoutState: StateFlow<LogoutUiState> = _logoutState

    /**
     * Pushes anything unsynced first (the before-logout sync). If the
     * push fails, nothing happens until the user confirms the warning
     * via [confirmLogoutAndDelete].
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

    /** "Log out and delete": discard the unsynced changes and sign out. */
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
        if (failed.afterWarning) confirmLogoutAndDelete() else logout()
    }

    fun cancelLogout() {
        _logoutState.value = LogoutUiState.Idle
    }

    private suspend fun finishLogout() {
        _logoutState.value = LogoutUiState.Idle
        _logoutEvent.emit(Unit)
    }

    /**
     * Set up password for Google-only account
     */
    fun setupPassword(password: String, onResult: (Boolean, String?) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onResult(false, "Not signed in")
            return
        }

        val email = user.email
        if (email.isNullOrEmpty()) {
            onResult(false, "No email associated with account")
            return
        }

        val credential = EmailAuthProvider.getCredential(email, password)
        user.linkWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    refreshProfile() // Update UI state
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Failed to set password")
                }
            }
    }

    /**
     * Change password for account that already has email/password
     */
    fun changePassword(currentPassword: String, newPassword: String, onResult: (Boolean, String?) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onResult(false, "Not signed in")
            return
        }

        val email = user.email
        if (email.isNullOrEmpty()) {
            onResult(false, "No email associated with account")
            return
        }

        // Re-authenticate with current password first
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    // Now update password
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                onResult(true, null)
                            } else {
                                onResult(false, updateTask.exception?.localizedMessage ?: "Failed to update password")
                            }
                        }
                } else {
                    onResult(false, "Current password is incorrect")
                }
            }
    }

    fun isPasswordStrong(password: String): Boolean = password.isStrongPassword()

    private val _accountDeletedEvent = MutableSharedFlow<Unit>()
    val accountDeletedEvent: SharedFlow<Unit> = _accountDeletedEvent.asSharedFlow()

    private val _accountDeletionState = MutableStateFlow<AccountDeletionUiState>(AccountDeletionUiState.Idle)
    val accountDeletionState: StateFlow<AccountDeletionUiState> = _accountDeletionState

    private val _deletionUnsyncedLines = MutableStateFlow<List<String>>(emptyList())
    /** Unsynced changes the delete will discard, for the confirmation dialog. Empty when there are none. */
    val deletionUnsyncedLines: StateFlow<List<String>> = _deletionUnsyncedLines

    /** Call when the delete confirmation dialog opens. */
    fun loadDeletionWarning() {
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
                        _accountDeletedEvent.emit(Unit)
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
        if (failed.canRetry) deleteAccount()
    }

    fun dismissAccountDeletion() {
        _accountDeletionState.value = AccountDeletionUiState.Idle
    }

    /**
     * Delete account with password re-authentication (for email/password users)
     */
    fun deleteAccountWithPassword(password: String, onResult: (Boolean, String?) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onResult(false, "Not signed in")
            return
        }

        val email = user.email
        if (email.isNullOrEmpty()) {
            onResult(false, "No email associated with account")
            return
        }

        // Re-authenticate first
        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    onResult(true, null)
                    deleteAccount()
                } else {
                    onResult(false, "Incorrect password")
                }
            }
    }

    /**
     * Delete account with Google re-authentication (for Google-only users)
     */
    fun deleteAccountWithGoogle(context: Context, webClientId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val idToken = getGoogleIdToken(context, webClientId)
                if (idToken != null) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user == null) {
                        onResult(false, "Not signed in")
                        return@launch
                    }

                    user.reauthenticate(credential)
                        .addOnCompleteListener { authTask ->
                            if (authTask.isSuccessful) {
                                onResult(true, null)
                                deleteAccount()
                            } else {
                                onResult(false, authTask.exception?.localizedMessage ?: "Re-authentication failed")
                            }
                        }
                } else {
                    onResult(false, "Could not get Google credentials")
                }
            } catch (e: GetCredentialCancellationException) {
                onResult(false, "Google Sign-In cancelled")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Google re-authentication failed")
            }
        }
    }

    private suspend fun getGoogleIdToken(context: Context, webClientId: String): String? {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialResult = credentialManager.getCredential(context, request)
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credentialResult.credential.data)
        return googleIdTokenCredential.idToken
    }

    private companion object {
        const val TAG = "SettingsViewModel"
        const val LOGOUT_FAILED_MESSAGE = "Couldn't log out. Please try again."
        const val DELETE_OFFLINE_MESSAGE = "Couldn't reach the server. Nothing was deleted. Check your connection and try again."
        const val DELETE_REAUTH_MESSAGE = "Your sign-in has expired. Close this and confirm your identity again."
        const val DELETE_FAILED_MESSAGE = "Couldn't delete your account. Please try again."
    }
}
