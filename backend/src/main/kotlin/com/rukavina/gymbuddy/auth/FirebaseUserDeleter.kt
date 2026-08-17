package com.rukavina.gymbuddy.auth

/**
 * The one point of contact with the Firebase Admin SDK for account
 * deletion. An interface, not a direct FirebaseAuth dependency in
 * AccountDeletionService, for the same reason TokenVerifier exists:
 * FirebaseAuth itself is excluded under the "test" profile (see
 * FirebaseAdminConfig), and account-deletion tests need to assert this
 * was called with the right uid without touching real Firebase.
 */
interface FirebaseUserDeleter {
    /** @throws FirebaseUserDeletionException if the Admin SDK rejects the call. */
    fun deleteUser(uid: String)
}

class FirebaseUserDeletionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
