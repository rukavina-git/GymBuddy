package com.rukavina.gymbuddy.data.auth

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** What went wrong, as the sign-in screens word it (Figma 114:20520). */
enum class AuthError {
    /** Never says which of the two is wrong (enumeration protection). */
    WRONG_CREDENTIALS,
    OFFLINE,
    EMAIL_IN_USE,
    WEAK_PASSWORD,
    INVALID_EMAIL,
    OTHER
}

class AuthException(val error: AuthError, cause: Throwable? = null) : Exception(error.name, cause)

/** A Google sign-in tried on the side, before the app's session sees it. */
data class GoogleProbe(val email: String?, val isNew: Boolean)

/**
 * Firebase sign-in for the auth screens. Every call throws [AuthException]
 * on failure. The real session is the default FirebaseApp's; AuthGate
 * listens to it, so anything that must not reach the gate goes through
 * the probe.
 */
interface AuthClient {
    /**
     * Signs [idToken] in on a separate FirebaseApp, so a Google account
     * that has no GymBuddy account yet isn't created behind the user's
     * back ("No account for this email yet").
     */
    suspend fun probeGoogle(idToken: String): GoogleProbe

    /** Ends the probe; [delete] removes the account the probe just created. */
    suspend fun endProbe(delete: Boolean)

    /** True when Firebase made a new account (the create flow then runs setup). */
    suspend fun signInWithGoogle(idToken: String): Boolean
    suspend fun signInWithEmail(email: String, password: String)
    suspend fun createWithEmail(email: String, password: String)
    suspend fun sendPasswordReset(email: String)
}

@Singleton
class FirebaseAuthClient @Inject constructor(
    @ApplicationContext private val context: Context
) : AuthClient {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    private val probe: FirebaseAuth by lazy {
        val app = FirebaseApp.getApps(context).firstOrNull { it.name == PROBE_APP }
            ?: FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, PROBE_APP)
        FirebaseAuth.getInstance(app)
    }

    override suspend fun probeGoogle(idToken: String): GoogleProbe = mapped {
        val result = probe.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        GoogleProbe(result.user?.email, result.additionalUserInfo?.isNewUser == true)
    }

    override suspend fun endProbe(delete: Boolean) {
        runCatching { if (delete) probe.currentUser?.delete()?.await() }
        probe.signOut()
    }

    override suspend fun signInWithGoogle(idToken: String): Boolean = mapped {
        val result = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        result.additionalUserInfo?.isNewUser == true
    }

    override suspend fun signInWithEmail(email: String, password: String) = mapped {
        auth.signInWithEmailAndPassword(email, password).await()
        Unit
    }

    override suspend fun createWithEmail(email: String, password: String) = mapped {
        auth.createUserWithEmailAndPassword(email, password).await()
        Unit
    }

    override suspend fun sendPasswordReset(email: String) = mapped {
        auth.sendPasswordResetEmail(email).await()
        Unit
    }

    private suspend fun <T> mapped(block: suspend () -> T): T = try {
        block()
    } catch (e: AuthException) {
        throw e
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw AuthException(classifyAuthError(e), e)
    }

    private companion object {
        const val PROBE_APP = "sign-in-probe"
    }
}

/** Firebase's exceptions to the screen's wording. Unknown errors are [AuthError.OTHER]. */
fun classifyAuthError(e: Throwable): AuthError = when (e) {
    is FirebaseNetworkException, is IOException -> AuthError.OFFLINE
    is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD
    is FirebaseAuthUserCollisionException -> AuthError.EMAIL_IN_USE
    is FirebaseAuthInvalidCredentialsException ->
        if (e.errorCode == "ERROR_INVALID_EMAIL") AuthError.INVALID_EMAIL else AuthError.WRONG_CREDENTIALS
    is FirebaseAuthInvalidUserException -> AuthError.WRONG_CREDENTIALS
    else -> AuthError.OTHER
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        val error = task.exception
        if (error != null) cont.resumeWithException(error)
        else if (task.isCanceled) cont.resumeWithException(IOException("cancelled"))
        else cont.resume(task.result)
    }
}
