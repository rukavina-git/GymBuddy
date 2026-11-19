package com.rukavina.gymbuddy.data.sync

import android.app.Application
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.concurrent.ExecutionException

/**
 * A failed ID token lookup (Crashlytics, 0.1.0/0.2.0): offline it fails
 * the request as an IOException; a dead session sends no token, so the
 * server's 401 reaches AuthGate. Robolectric: Firebase's exceptions need
 * android.text.TextUtils.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TokenAfterFailureTest {

    private val remembered = mutableListOf<AuthProbe>()

    @Test
    fun `offline refresh is thrown as IOException and says nothing about the session`() {
        val offline = ExecutionException(FirebaseNetworkException("op1: A network error has occurred."))

        try {
            tokenAfterFailure(offline, remembered::add)
            fail("expected IOException")
        } catch (e: IOException) {
            assertSame(offline, e.cause)
        }
        assertEquals(emptyList<AuthProbe>(), remembered)
    }

    @Test
    fun `unknown failure is thrown as IOException too`() {
        assertThrows(IOException::class.java) {
            tokenAfterFailure(ExecutionException(IllegalStateException("?")), remembered::add)
        }
        assertEquals(emptyList<AuthProbe>(), remembered)
    }

    @Test
    fun `expired session gives no token and is remembered`() {
        val expired = ExecutionException(FirebaseAuthException("ERROR_USER_TOKEN_EXPIRED", "expired"))

        assertNull(tokenAfterFailure(expired, remembered::add))
        assertEquals(listOf(AuthProbe.EXPIRED), remembered)
    }

    @Test
    fun `deleted user gives no token and is remembered`() {
        val deleted = ExecutionException(FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "gone"))

        assertNull(tokenAfterFailure(deleted, remembered::add))
        assertEquals(listOf(AuthProbe.DELETED), remembered)
    }
}
