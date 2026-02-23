package com.rukavina.gymbuddy.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.rukavina.gymbuddy.utils.validation.isStrongPassword
import com.rukavina.gymbuddy.utils.validation.isValidUsername

private const val TAG = "AuthViewModel"

/**
 * Result of authentication operations.
 */
sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
    data class AccountExists(val email: String, val isGoogleAccount: Boolean) : AuthResult()
}

/**
 * ViewModel for registration operations.
 * Login operations are handled by LoginViewModel.
 */
class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

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
                        Log.d(TAG, "Registration: account already exists")
                        onComplete(AuthResult.AccountExists(email, isGoogleAccount = false))
                    } else {
                        Log.e(TAG, "User registration failed", exception)
                        onComplete(AuthResult.Error(exception?.localizedMessage ?: "Registration failed"))
                    }
                }
            }
    }

    fun isPasswordStrong(password: String): Boolean = password.isStrongPassword()

    fun isUsernameValid(username: String): Boolean = username.isValidUsername()
}
