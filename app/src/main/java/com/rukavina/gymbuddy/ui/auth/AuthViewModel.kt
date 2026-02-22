package com.rukavina.gymbuddy.ui.auth

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider

private const val TAG = "AuthViewModel"

sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
    data class AccountExists(val email: String, val isGoogleAccount: Boolean) : AuthResult()
}

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    // Store pending credential for account linking
    var pendingGoogleCredential = mutableStateOf<AuthCredential?>(null)
        private set

    fun signInWithGoogle(idToken: String, onComplete: (AuthResult) -> Unit) {
        Log.d(TAG, "Google sign-in initiated")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Google sign-in successful")
                    pendingGoogleCredential.value = null
                    onComplete(AuthResult.Success)
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        // Account exists with different provider (email/password)
                        Log.d(TAG, "Google sign-in: account exists with email/password")
                        pendingGoogleCredential.value = credential
                        val email = exception.email ?: ""
                        onComplete(AuthResult.AccountExists(email, isGoogleAccount = false))
                    } else {
                        Log.e(TAG, "Google sign-in failed", exception)
                        onComplete(AuthResult.Error(exception?.localizedMessage ?: "Google Sign-In failed"))
                    }
                }
            }
    }

    fun loginAndLinkGoogle(email: String, password: String, onComplete: (AuthResult) -> Unit) {
        Log.d(TAG, "Login and link Google account initiated")
        val googleCredential = pendingGoogleCredential.value
        if (googleCredential == null) {
            Log.e(TAG, "No pending Google credential for linking")
            onComplete(AuthResult.Error("No pending Google credential"))
            return
        }

        // First sign in with email/password
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { signInTask ->
                if (signInTask.isSuccessful) {
                    // Now link the Google credential
                    auth.currentUser?.linkWithCredential(googleCredential)
                        ?.addOnCompleteListener { linkTask ->
                            pendingGoogleCredential.value = null
                            if (linkTask.isSuccessful) {
                                Log.d(TAG, "Google account linked successfully")
                                onComplete(AuthResult.Success)
                            } else {
                                // Link failed but user is signed in
                                Log.d(TAG, "Google link failed but user signed in")
                                onComplete(AuthResult.Success)
                            }
                        }
                } else {
                    Log.e(TAG, "Login for linking failed", signInTask.exception)
                    onComplete(AuthResult.Error(signInTask.exception?.localizedMessage ?: "Login failed"))
                }
            }
    }

    fun registerUser(email: String, password: String, username: String, onComplete: (AuthResult) -> Unit) {
        Log.d(TAG, "User registration initiated")
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "User registration successful")
                    onComplete(AuthResult.Success)
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        // Account already exists - user should try signing in
                        Log.d(TAG, "Registration: account already exists")
                        onComplete(AuthResult.AccountExists(email, isGoogleAccount = false))
                    } else {
                        Log.e(TAG, "User registration failed", exception)
                        onComplete(AuthResult.Error(exception?.localizedMessage ?: "Registration failed"))
                    }
                }
            }
    }

    fun loginUser(email: String, password: String, onComplete: (AuthResult) -> Unit) {
        Log.d(TAG, "User login initiated")
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "User login successful")
                    onComplete(AuthResult.Success)
                } else {
                    Log.e(TAG, "User login failed", task.exception)
                    onComplete(AuthResult.Error(task.exception?.localizedMessage ?: "Login failed"))
                }
            }
    }

    fun isPasswordStrong(password: String): Boolean {
        return isStrongPassword(password)
    }

    fun isUsernameValid(username: String): Boolean {
        return isValidUsername(username)
    }

    private fun isValidUsername(username: String): Boolean {
        return username.length >= 4 && username.matches(Regex("^[a-z0-9]+$"))
    }

    private fun isStrongPassword(password: String): Boolean {
        val minLength = 8
        val hasUppercase = Regex("[A-Z]").containsMatchIn(password)
        val hasLowercase = Regex("[a-z]").containsMatchIn(password)
        val hasDigit = Regex("[0-9]").containsMatchIn(password)

        return password.length >= minLength &&
                hasUppercase &&
                hasLowercase &&
                hasDigit
    }

    fun clearPendingCredential() {
        pendingGoogleCredential.value = null
    }

    fun logoutUser() {
        Log.d(TAG, "User logged out")
        pendingGoogleCredential.value = null
        auth.signOut()
    }
}
