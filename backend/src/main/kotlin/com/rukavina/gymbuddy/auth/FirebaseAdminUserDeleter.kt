package com.rukavina.gymbuddy.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/** Production FirebaseUserDeleter. Delegates entirely to the Admin SDK's synchronous deleteUser. */
@Component
@Profile("!test")
class FirebaseAdminUserDeleter(private val firebaseAuth: FirebaseAuth) : FirebaseUserDeleter {

    override fun deleteUser(uid: String) {
        try {
            firebaseAuth.deleteUser(uid)
        } catch (e: FirebaseAuthException) {
            throw FirebaseUserDeletionException("Firebase rejected deleteUser for uid=$uid (${e.authErrorCode}).", e)
        }
    }
}
