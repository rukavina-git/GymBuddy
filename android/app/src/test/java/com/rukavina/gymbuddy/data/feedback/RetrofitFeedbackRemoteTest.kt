package com.rukavina.gymbuddy.data.feedback

import com.rukavina.gymbuddy.data.remote.generated.models.FeedbackRequest
import com.rukavina.gymbuddy.data.sync.FakeAuthSession
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/** A message is sent under its author's account only while that author is signed in. */
class RetrofitFeedbackRemoteTest {

    private val server = MockWebServer()
    private val auth = FakeAuthSession(currentUid = "uid-1")
    private val request = FeedbackRequest(UUID.randomUUID(), "hi", 1L, "0.2.4", "Android 15", "samsung SM-S731B")

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private suspend fun sendAs(author: String?): String? {
        server.enqueue(MockResponse.Builder().code(204).build())
        val status = RetrofitFeedbackRemote(server.url("/").toString(), auth).send(request, author)
        assertEquals(204, status)
        val recorded = server.takeRequest()
        assertEquals("/v1/feedback", recorded.url.encodedPath)
        assertTrue(recorded.body!!.utf8().contains("\"message\":\"hi\""))
        return recorded.headers["Authorization"]
    }

    @Test
    fun `author still signed in sends the token`() = runTest {
        assertEquals("Bearer token", sendAs("uid-1"))
    }

    @Test
    fun `another account signed in sends as guest`() = runTest {
        auth.currentUid = "uid-2"
        assertNull(sendAs("uid-1"))
    }

    @Test
    fun `author signed out since sends as guest`() = runTest {
        auth.currentUid = null
        assertNull(sendAs("uid-1"))
    }

    @Test
    fun `guest message never carries a token`() = runTest {
        assertNull(sendAs(null))
    }
}
