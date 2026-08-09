package com.rukavina.gymbuddy.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "LoginViewModel"

/**
 * Events emitted by LoginViewModel for one-time actions.
 */
sealed class LoginEvent {
    data object LoginSuccess : LoginEvent()
    data class ShowError(val message: String) : LoginEvent()
}

class LoginViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    // Store pending Google credential for account linking
    private var pendingGoogleCredential: AuthCredential? = null

    // Form actions
    fun updateEmail(email: String) {
        _uiState.update { it.copy(form = it.form.copy(email = email)) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(form = it.form.copy(password = password)) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(form = it.form.copy(isPasswordVisible = !it.form.isPasswordVisible)) }
    }

    // Login with email/password
    fun loginWithEmailPassword() {
        val email = _uiState.value.form.email
        val password = _uiState.value.form.password

        if (email.isEmpty() || password.isEmpty()) {
            viewModelScope.launch {
                _events.emit(LoginEvent.ShowError("Email and password cannot be empty"))
            }
            return
        }

        _uiState.update { it.copy(loading = it.loading.copy(isLoginLoading = true)) }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _uiState.update { it.copy(loading = it.loading.copy(isLoginLoading = false)) }

                if (task.isSuccessful) {
                    Log.d(TAG, "Login successful")
                    viewModelScope.launch { _events.emit(LoginEvent.LoginSuccess) }
                } else {
                    Log.e(TAG, "Login failed", task.exception)
                    // Show dialog suggesting Google sign-in
                    _uiState.update { it.copy(showLoginFailedDialog = true) }
                }
            }
    }

    // Google sign-in
    fun signInWithGoogle(context: Context, webClientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = it.loading.copy(isGoogleSignInLoading = true)) }

            try {
                val idToken = getGoogleIdToken(context, webClientId)
                if (idToken != null) {
                    authenticateWithGoogle(idToken)
                } else {
                    _uiState.update { it.copy(loading = it.loading.copy(isGoogleSignInLoading = false)) }
                }
            } catch (e: GetCredentialCancellationException) {
                Log.d(TAG, "Google Sign-In cancelled")
                _uiState.update { it.copy(loading = it.loading.copy(isGoogleSignInLoading = false)) }
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                _uiState.update { it.copy(loading = it.loading.copy(isGoogleSignInLoading = false)) }
                _events.emit(LoginEvent.ShowError("Google Sign-In failed: ${e.message}"))
            }
        }
    }

    private suspend fun getGoogleIdToken(context: Context, webClientId: String): String? {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialResult = credentialManager.getCredential(context, request)
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credentialResult.credential.data)
        return googleIdTokenCredential.idToken
    }

    private fun authenticateWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                _uiState.update { it.copy(loading = it.loading.copy(isGoogleSignInLoading = false)) }

                if (task.isSuccessful) {
                    Log.d(TAG, "Google sign-in successful")
                    pendingGoogleCredential = null
                    viewModelScope.launch { _events.emit(LoginEvent.LoginSuccess) }
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        // Account exists with email/password, show link dialog
                        Log.d(TAG, "Google sign-in: account exists with email/password")
                        pendingGoogleCredential = credential
                        _uiState.update {
                            it.copy(
                                linkDialog = LinkDialogState(
                                    isVisible = true,
                                    email = exception.email ?: ""
                                )
                            )
                        }
                    } else {
                        Log.e(TAG, "Google sign-in failed", exception)
                        viewModelScope.launch {
                            _events.emit(LoginEvent.ShowError(exception?.localizedMessage ?: "Google Sign-In failed"))
                        }
                    }
                }
            }
    }

    // Link dialog actions
    fun updateLinkPassword(password: String) {
        _uiState.update { it.copy(linkDialog = it.linkDialog.copy(password = password)) }
    }

    fun toggleLinkPasswordVisibility() {
        _uiState.update { it.copy(linkDialog = it.linkDialog.copy(isPasswordVisible = !it.linkDialog.isPasswordVisible)) }
    }

    fun dismissLinkDialog() {
        pendingGoogleCredential = null
        _uiState.update { it.copy(linkDialog = LinkDialogState()) }
    }

    fun loginAndLinkGoogle() {
        val email = _uiState.value.linkDialog.email
        val password = _uiState.value.linkDialog.password
        val googleCredential = pendingGoogleCredential

        if (googleCredential == null) {
            viewModelScope.launch {
                _events.emit(LoginEvent.ShowError("No pending Google credential"))
            }
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { signInTask ->
                if (signInTask.isSuccessful) {
                    // Link the Google credential
                    auth.currentUser?.linkWithCredential(googleCredential)
                        ?.addOnCompleteListener { linkTask ->
                            pendingGoogleCredential = null
                            _uiState.update { it.copy(linkDialog = LinkDialogState()) }

                            if (linkTask.isSuccessful) {
                                Log.d(TAG, "Google account linked successfully")
                            } else {
                                Log.d(TAG, "Google link failed but user signed in")
                            }
                            viewModelScope.launch { _events.emit(LoginEvent.LoginSuccess) }
                        }
                } else {
                    Log.e(TAG, "Login for linking failed", signInTask.exception)
                    viewModelScope.launch {
                        _events.emit(LoginEvent.ShowError(signInTask.exception?.localizedMessage ?: "Login failed"))
                    }
                }
            }
    }

    // Login failed dialog
    fun dismissLoginFailedDialog() {
        _uiState.update { it.copy(showLoginFailedDialog = false) }
    }

    fun signInWithGoogleFromFailedDialog(context: Context, webClientId: String) {
        _uiState.update { it.copy(showLoginFailedDialog = false) }
        signInWithGoogle(context, webClientId)
    }
}
