package com.rukavina.gymbuddy.data.sync

import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.rukavina.gymbuddy.data.crash.CrashReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.ExecutionException

/** The signed-in user, as far as sync is concerned. */
interface AuthSession {
    val currentUid: String?
    suspend fun idToken(): String?

    /** A voluntary sign-out: the data owner is released first (see [DataOwnerStore]). */
    fun signOut()
}

/** What a forced token refresh says about the session (AuthGate). */
enum class AuthProbe {
    VALID,

    /** No answer: offline or Firebase unreachable. Says nothing about the session. */
    OFFLINE,

    /** Refresh token revoked or expired (password changed, signed out elsewhere). */
    EXPIRED,

    /** The Firebase user is gone or disabled. */
    DELETED;

    companion object {
        private val DELETED_CODES = setOf("ERROR_USER_NOT_FOUND", "ERROR_USER_DISABLED")

        /** Firebase's token errors, by error code; anything unknown counts as [OFFLINE] (do nothing). */
        fun classify(error: Throwable): AuthProbe {
            val cause = (error as? ExecutionException)?.cause ?: error
            return when {
                cause is FirebaseNetworkException || cause is IOException -> OFFLINE
                cause is FirebaseAuthInvalidUserException && cause.errorCode in DELETED_CODES -> DELETED
                cause is FirebaseAuthException -> EXPIRED
                else -> OFFLINE
            }
        }
    }
}

/**
 * Firebase-backed [AuthSession]. FirebaseAuth is resolved on first use,
 * not at injection, so building the object graph never requires an
 * initialised FirebaseApp.
 *
 * [signOut] here is Firebase's alone; the app's [AuthSession] wraps it
 * with the owner release (SyncModule). AuthGate calls this one when the
 * session is already dead.
 */
class FirebaseAuthSession(authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() }) : AuthSession {

    private val auth by lazy(authProvider)

    // Signed out if Firebase itself isn't available, rather than crashing a trigger.
    override val currentUid: String? get() = runCatching { auth.currentUser?.uid }.getOrNull()

    val currentEmail: String? get() = runCatching { auth.currentUser?.email }.getOrNull()

    /**
     * The last token failure that said something about the session. Firebase
     * signs the user out by itself on some of them, before AuthGate can ask,
     * so the gate reads this to tell expired from deleted.
     */
    @Volatile
    var lastFailure: AuthProbe? = null
        private set

    override suspend fun idToken(): String? {
        val user = auth.currentUser ?: return null
        // Firebase refreshes the token itself when it's near expiry.
        return try {
            withContext(Dispatchers.IO) { Tasks.await(user.getIdToken(false)).token }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            remember(AuthProbe.classify(e))
            throw e
        }
    }

    /** Forces a token refresh, which is what tells expired, deleted and offline apart. */
    suspend fun probe(): AuthProbe {
        val user = auth.currentUser ?: return lastFailure ?: AuthProbe.EXPIRED
        return try {
            withContext(Dispatchers.IO) { Tasks.await(user.getIdToken(true)) }
            AuthProbe.VALID
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthProbe.classify(e).also(::remember)
        }
    }

    private fun remember(probe: AuthProbe) {
        if (probe == AuthProbe.EXPIRED || probe == AuthProbe.DELETED) lastFailure = probe
    }

    /** A new sign-in starts with a clean slate. */
    fun clearFailure() {
        lastFailure = null
    }

    override fun signOut() = auth.signOut()

    private val uids = MutableStateFlow<String?>(null)

    /** The signed-in uid as Firebase last reported it; set once [observeSignIn] runs. */
    val signedInUid: StateFlow<String?> = uids.asStateFlow()

    /**
     * Hands every uid change to [onUid] (AuthGate), one at a time, in order;
     * a quick A → B only needs to end on B, so the flow conflates. Also
     * keeps crash reports attributed to the signed-in uid.
     */
    fun observeSignIn(
        crashReporter: CrashReporter,
        onUid: suspend (uid: String?) -> Unit,
        scope: CoroutineScope
    ) {
        uids.value = currentUid
        var reportedUid: String? = null
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid != reportedUid) crashReporter.setUserId(uid)
            reportedUid = uid
            uids.value = uid
        }
        scope.launch {
            uids.collect { uid -> onUid(uid) }
        }
    }
}

/** The app's [AuthSession]: a sign-out through it is voluntary, so it releases the data owner first. */
class OwnerReleasingAuthSession(
    private val firebase: FirebaseAuthSession,
    private val owners: DataOwnerStore
) : AuthSession by firebase {
    override fun signOut() {
        owners.release()
        firebase.signOut()
    }
}
