package com.rukavina.gymbuddy.data.sync.remote

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.ExecutionException

/**
 * A token lookup that fails offline must fail the call, not the app:
 * OkHttp rethrows non-IOExceptions on its worker thread (Crashlytics,
 * 0.2.0: ExecutionException from FirebaseAuthSession.idToken).
 */
class BearerTokenInterceptorTest {

    private val server = MockWebServer()
    private val uncaught = mutableListOf<Throwable>()
    private val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

    @Before
    fun setUp() {
        server.start()
        Thread.setDefaultUncaughtExceptionHandler { _, e -> synchronized(uncaught) { uncaught += e } }
    }

    @After
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        server.close()
    }

    @Test
    fun `offline token refresh fails the call with an IOException and crashes nothing`() = runTest {
        val offline = ExecutionException(RuntimeException("op1: network error")) // Firebase wraps its own type here
        val apis = SyncApis.create(server.url("/").toString(), { throw offline })

        try {
            apis.account.deleteAccount()
            fail("expected IOException")
        } catch (e: IOException) {
            // coroutine stack-trace recovery may add a copy, so look down the cause chain
            val original = generateSequence(e.cause) { it.cause }.filterIsInstance<ExecutionException>().firstOrNull()
            assertEquals(offline.message, original?.message)
        }

        Thread.sleep(200) // give OkHttp's worker a chance to rethrow, as it did before the fix
        assertEquals(emptyList<Throwable>(), synchronized(uncaught) { uncaught.toList() })
        assertEquals(0, server.requestCount)
    }
}
