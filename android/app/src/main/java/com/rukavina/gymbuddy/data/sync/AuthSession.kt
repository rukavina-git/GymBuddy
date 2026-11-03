package com.rukavina.gymbuddy.data.sync

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.rukavina.gymbuddy.data.crash.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val uids = MutableStateFlow<String?>(null)

    /** The signed-in uid as Firebase last reported it; set once [observeSignIn] runs. */
    val signedInUid: StateFlow<String?> = uids.asStateFlow()

    /**
     * On every uid change, first opens that owner's database
     * ([switchOwner], ADR 0014), then runs [afterSwitch] (the sign-in
     * sync and the guest merge). Also keeps crash reports attributed to
     * the signed-in uid, cleared on sign-out.
     *
     * Changes are handled one at a time, in order; a quick A → B only
     * needs to end on B, so the flow conflates.
     */
    fun observeSignIn(
        crashReporter: CrashReporter,
        switchOwner: suspend (uid: String?) -> Unit,
        afterSwitch: suspend (uid: String?) -> Unit,
        scope: CoroutineScope
    ) {
        // Starts at the restored uid, which DatabaseHolder already opened:
        // a cold start must not pass through the guest file.
        uids.value = currentUid
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
                afterSwitch(uid)
            }
        }
    }
}
