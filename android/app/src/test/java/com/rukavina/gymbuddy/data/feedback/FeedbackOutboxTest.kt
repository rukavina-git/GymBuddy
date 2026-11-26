package com.rukavina.gymbuddy.data.feedback

import androidx.room.Room
import com.rukavina.gymbuddy.data.remote.generated.models.FeedbackRequest
import com.rukavina.gymbuddy.domain.id.Uuid7Generator
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Plan 10 step 06b: Send never loses a message; the queue drains on 2xx
 * and on rejections. Real time (runBlocking): Room runs on its own
 * executor, and runTest's virtual clock would fire the send timeout
 * while waiting for it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedbackOutboxTest {

    private lateinit var db: FeedbackDatabase
    private val clock = Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneOffset.UTC)
    private val device = FeedbackDevice("0.2.4", "Android 15", "samsung SM-S731B")
    private var uid: String? = "uid-1"
    private var scheduled = 0
    private val sent = mutableListOf<Pair<FeedbackRequest, String?>>()

    /** Answers for the next sends, in order; past the end every send gets 204. */
    private val answers = ArrayDeque<suspend () -> Int>()

    private val remote = FeedbackRemote { request, author ->
        sent += request to author
        (answers.removeFirstOrNull() ?: { 204 })()
    }

    private fun outbox(timeoutMs: Long = FeedbackOutbox.IMMEDIATE_TIMEOUT_MS) = FeedbackOutbox(
        dao = { db.pendingFeedbackDao() },
        remote = remote,
        ids = Uuid7Generator(clock),
        clock = clock,
        device = device,
        currentUid = { uid },
        scheduleSend = { scheduled++ },
        immediateTimeoutMs = timeoutMs,
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), FeedbackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun queued() = db.pendingFeedbackDao().all()

    @Test
    fun `online send goes out at once with device info and author`() = runBlocking {
        val result = outbox().submit("  Rest timer keeps running.\nThanks  ")

        assertEquals(FeedbackSubmitResult.SENT, result)
        assertTrue(queued().isEmpty())
        assertEquals(0, scheduled)
        val (request, author) = sent.single()
        assertEquals("Rest timer keeps running.\nThanks", request.message)
        assertEquals("0.2.4", request.appVersion)
        assertEquals("Android 15", request.osVersion)
        assertEquals("samsung SM-S731B", request.deviceModel)
        assertEquals(clock.millis(), request.createdAt)
        assertEquals("uid-1", author)
    }

    @Test
    fun `guest message has no author`() = runBlocking {
        uid = null
        outbox().submit("hi")
        assertNull(sent.single().second)
    }

    @Test
    fun `offline keeps the message and schedules the worker, which sends it later with the same id`() = runBlocking {
        answers += { throw IOException("offline") }
        val outbox = outbox()

        assertEquals(FeedbackSubmitResult.QUEUED, outbox.submit("hi"))
        assertEquals(1, queued().size)
        assertEquals(1, scheduled)

        assertTrue(outbox.flush())
        assertTrue(queued().isEmpty())
        assertEquals("retry reuses the id, so the server dedupes", sent[0].first.id, sent[1].first.id)
    }

    @Test
    fun `429 and 5xx keep the message and stop the run`() = runBlocking {
        answers += { throw IOException("offline") }
        answers += { throw IOException("offline") }
        val outbox = outbox()
        outbox.submit("first")
        outbox.submit("second")
        sent.clear()

        answers += { 429 }
        assertFalse(outbox.flush())
        answers += { 503 }
        assertFalse(outbox.flush())

        assertEquals(2, queued().size)
        assertEquals("stops at the first failure, oldest first", listOf("first", "first"), sent.map { it.first.message })
    }

    @Test
    fun `a rejected message is dropped and the run goes on`() = runBlocking {
        answers += { throw IOException("offline") }
        answers += { throw IOException("offline") }
        val outbox = outbox()
        outbox.submit("first")
        outbox.submit("second")

        answers += { 400 }
        answers += { 204 }
        assertTrue(outbox.flush())
        assertTrue(queued().isEmpty())
    }

    @Test
    fun `a slow network hands over to the worker instead of keeping the user waiting`() = runBlocking {
        answers += { delay(5_000); 204 }

        val result = outbox(timeoutMs = 200).submit("hi")

        assertEquals(FeedbackSubmitResult.QUEUED, result)
        assertEquals(1, queued().size)
        assertEquals(1, scheduled)
    }

    @Test
    fun `account deletion removes only that account's unsent messages`() = runBlocking {
        answers += { throw IOException("offline") }
        answers += { throw IOException("offline") }
        val outbox = outbox()
        outbox.submit("mine")
        uid = null
        outbox.submit("guest")

        outbox.deleteForAccount("uid-1")

        assertEquals(listOf("guest"), queued().map { it.message })
    }

    @Test
    fun `blank or too long is refused before anything is stored`() = runBlocking {
        val outbox = outbox()
        assertThrows(IllegalArgumentException::class.java) { runBlocking { outbox.submit("   ") } }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { outbox.submit("x".repeat(FEEDBACK_MAX_LENGTH + 1)) }
        }
        assertTrue(queued().isEmpty())
        assertTrue(sent.isEmpty())
    }
}
