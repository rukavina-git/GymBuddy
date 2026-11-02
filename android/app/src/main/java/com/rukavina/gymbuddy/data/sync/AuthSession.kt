package com.rukavina.gymbuddy.data.sync

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.rukavina.gymbuddy.data.crash.CrashReporter
import com.rukavina.gymbuddy.domain.sync.SyncReason
import com.rukavina.gymbuddy.domain.sync.SyncRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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

    /**
     * On every uid change, first opens that owner's database
     * ([switchOwner], ADR 0014), then syncs if someone signed in, so a
     * fresh install or a new account pulls its data straight away. Also
     * keeps crash reports attributed to the signed-in uid, cleared on
     * sign-out.
     *
     * Changes are handled one at a time, in order; a quick A → B only
     * needs to end on B, so the flow conflates.
     */
    fun observeSignIn(
        requester: SyncRequester,
        crashReporter: CrashReporter,
        switchOwner: suspend (uid: String?) -> Unit,
        scope: CoroutineScope
    ) {
        // Starts at the restored uid, which DatabaseHolder already opened:
        // a cold start must not pass through the signed-out file.
        val uids = MutableStateFlow(currentUid)
        var reportedUid: String? = null
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid != reportedUid) crashReporter.setUserId(uid)
            reportedUid = uid
            uids.value = uid
        }
        scope.launch {
            uids.collect { uid ->
                switchOwner(uid)
                if (uid != null) requester.requestSync(SyncReason.SIGNED_IN)
            }
        }
    }
}
