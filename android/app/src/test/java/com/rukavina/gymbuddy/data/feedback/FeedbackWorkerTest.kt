package com.rukavina.gymbuddy.data.feedback

import android.content.Context
import androidx.room.Room
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.rukavina.gymbuddy.domain.id.Uuid7Generator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.Clock

/** The worker retries until the queue is empty. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedbackWorkerTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private lateinit var db: FeedbackDatabase
    private var answer: () -> Int = { 204 }

    private val outbox by lazy {
        FeedbackOutbox(
            dao = { db.pendingFeedbackDao() },
            remote = { _, _ -> answer() },
            ids = Uuid7Generator(Clock.systemUTC()),
            clock = Clock.systemUTC(),
            device = FeedbackDevice("0.2.4", "Android 15", "x"),
            currentUid = { null },
            scheduleSend = {},
        )
    }

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, FeedbackDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun run(): ListenableWorker.Result = runBlocking {
        TestListenableWorkerBuilder<FeedbackWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                    FeedbackWorker(appContext, workerParameters, outbox)
            })
            .build()
            .doWork()
    }

    private fun queueOne() = runBlocking {
        answer = { throw IOException("offline") }
        outbox.submit("hi")
    }

    @Test
    fun `offline retries`() {
        queueOne()
        assertEquals(ListenableWorker.Result.retry(), run())
    }

    @Test
    fun `rate limited retries`() {
        queueOne()
        answer = { 429 }
        assertEquals(ListenableWorker.Result.retry(), run())
    }

    @Test
    fun `sent succeeds and empties the queue`() {
        queueOne()
        answer = { 204 }
        assertEquals(ListenableWorker.Result.success(), run())
        assertEquals(0, runBlocking { db.pendingFeedbackDao().all().size })
    }
}
