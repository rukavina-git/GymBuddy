package com.rukavina.gymbuddy.data.sync

import android.app.Application
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * FirebaseAuthSession.idToken, the frame in the 0.1.0/0.2.0 crash: offline
 * the refresh throws IOException (a failed call, not a dead app); a dead
 * session gives no token and is remembered for AuthGate.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FirebaseAuthSessionTokenTest {

    private val user = mockk<FirebaseUser>()
    private val auth = mockk<FirebaseAuth> { every { currentUser } returns user }
    private val session = FirebaseAuthSession { auth }

    @Test
    fun `valid token is returned`() = runTest {
        val result = mockk<GetTokenResult> { every { token } returns "abc" }
        every { user.getIdToken(false) } returns Tasks.forResult(result)

        assertEquals("abc", session.idToken())
    }

    @Test
    fun `offline refresh throws IOException and leaves the session alone`() = runTest {
        every { user.getIdToken(false) } returns Tasks.forException(FirebaseNetworkException("offline"))

        val error = runCatching { session.idToken() }.exceptionOrNull()

        assertTrue("got $error", error is IOException)
        assertTrue(generateSequence(error) { it.cause }.any { it is FirebaseNetworkException })
        assertNull(session.lastFailure)
    }

    @Test
    fun `expired session gives no token and is remembered`() = runTest {
        every { user.getIdToken(false) } returns
            Tasks.forException(FirebaseAuthException("ERROR_USER_TOKEN_EXPIRED", "expired"))

        assertNull(session.idToken())
        assertEquals(AuthProbe.EXPIRED, session.lastFailure)
    }

    @Test
    fun `signed out gives no token without asking Firebase`() = runTest {
        every { auth.currentUser } returns null

        assertNull(session.idToken())
    }
}
