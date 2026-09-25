package com.rukavina.gymbuddy.data.sync

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.rukavina.gymbuddy.domain.sync.SyncReason
import com.rukavina.gymbuddy.domain.sync.SyncRequester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The signed-in user, as far as sync is concerned. */
interface AuthSession {
    val currentUid: String?
    suspend fun idToken(): String?
    fun signOut()
}

/**
 * Firebase-backed [AuthSession]. Also the sign-in sync trigger.
 * FirebaseAuth is resolved on first use, not at injection, so building
 * the object graph never requires an initialised FirebaseApp.
 */
class FirebaseAuthSession(authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() }) : AuthSession {

    private val auth by lazy(authProvider)

    // Signed out if Firebase itself isn't available, rather than crashing a trigger.
    override val currentUid: String? get() = runCatching { auth.currentUser?.uid }.getOrNull()

    override suspend fun idToken(): String? {
        val user = auth.currentUser ?: return null
        // Firebase refreshes the token itself when it's near expiry.
        return withContext(Dispatchers.IO) { Tasks.await(user.getIdToken(false)).token }
    }

    override fun signOut() = auth.signOut()

    /** Syncs as soon as someone signs in, so a fresh install or a new account pulls its data straight away. */
    fun observeSignIn(requester: SyncRequester) {
        var lastUid: String? = null
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid != null && uid != lastUid) requester.requestSync(SyncReason.SIGNED_IN)
            lastUid = uid
        }
    }
}
