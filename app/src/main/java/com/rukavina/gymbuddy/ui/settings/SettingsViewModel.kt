package com.rukavina.gymbuddy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val profileImageUri: String? = null,
    val isLoading: Boolean = true,
    val hasPassword: Boolean = false,
    val isGoogleOnly: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: UserProfileRepository
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
                        isGoogleOnly = isGoogleOnly
                    )
                } else {
                    SettingsUiState(
                        userName = "",
                        userEmail = firebaseEmail,
                        profileImageUri = null,
                        isLoading = false,
                        hasPassword = hasPassword,
                        isGoogleOnly = isGoogleOnly
                    )
                }
            } ?: run {
                _uiState.value = SettingsUiState(
                    userName = "",
                    userEmail = firebaseEmail,
                    profileImageUri = null,
                    isLoading = false,
                    hasPassword = hasPassword,
                    isGoogleOnly = isGoogleOnly
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
}
