package com.rukavina.gymbuddy.ui.settings

import android.content.Context
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
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.rukavina.gymbuddy.utils.validation.isStrongPassword
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val userName: String = "",
    val userEmail: String = "",
    val profileImageUri: String? = null,
    val isLoading: Boolean = true,
    val hasPassword: Boolean = false,
    val isGoogleOnly: Boolean = false,
    val hasGoogle: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: UserProfileRepository,
    private val database: AppDatabase,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    private val uid: String? = FirebaseAuth.getInstance().currentUser?.uid

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

            uid?.let { userId ->
                val profile = repository.getProfile(userId)
                _uiState.value = if (profile != null) {
                    SettingsUiState(
                        userName = profile.name,
                        userEmail = profile.email.ifBlank { firebaseEmail },
                        profileImageUri = profile.profileImageUrl,
                        isLoading = false,
                        hasPassword = hasPassword,
                        isGoogleOnly = isGoogleOnly,
                        hasGoogle = hasGoogle
                    )
                } else {
                    SettingsUiState(
                        userName = "",
                        userEmail = firebaseEmail,
                        profileImageUri = null,
                        isLoading = false,
                        hasPassword = hasPassword,
                        isGoogleOnly = isGoogleOnly,
                        hasGoogle = hasGoogle
                    )
                }
            } ?: run {
                _uiState.value = SettingsUiState(
                    userName = "",
                    userEmail = firebaseEmail,
                    profileImageUri = null,
                    isLoading = false,
                    hasPassword = hasPassword,
                    isGoogleOnly = isGoogleOnly,
                    hasGoogle = hasGoogle
                )
            }
        }
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        viewModelScope.launch {
            _logoutEvent.emit(Unit)
        }
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
                    // Now delete the account
                    performAccountDeletion(onResult)
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
                                performAccountDeletion(onResult)
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

    private fun performAccountDeletion(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                // Clear all local data
                withContext(Dispatchers.IO) {
                    // Clear database
                    database.clearAllTables()

                    // Delete profile images folder
                    val profileImagesDir = File(appContext.filesDir, "profile_images")
                    if (profileImagesDir.exists()) {
                        profileImagesDir.listFiles()?.forEach { it.delete() }
                        profileImagesDir.delete()
                    }
                }

                // Delete Firebase account
                val user = FirebaseAuth.getInstance().currentUser
                user?.delete()?.addOnCompleteListener { deleteTask ->
                    if (deleteTask.isSuccessful) {
                        viewModelScope.launch {
                            _accountDeletedEvent.emit(Unit)
                        }
                        onResult(true, null)
                    } else {
                        onResult(false, deleteTask.exception?.localizedMessage ?: "Failed to delete account")
                    }
                } ?: onResult(false, "User not found")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Failed to delete account data")
            }
        }
    }
}
