package com.rukavina.gymbuddy.data.sync

import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.repository.ExerciseRepositoryImpl
import com.rukavina.gymbuddy.data.sync.remote.AuthTokenProvider
import com.rukavina.gymbuddy.data.sync.remote.SyncApis
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/**
 * Whatever the network or sign-in does, a sync cycle must fail quietly:
 * no exception on any thread (OkHttp rethrows some on its own thread,
 * which kills the app: Crashlytics 0.1.0/0.2.0), and the unsynced local
 * change stays in the outbox for the next try. The real SyncEngine,
 * Retrofit/OkHttp stack and Room; only the server and token are faked.
 *
 * A new way for sync to fail goes here as one more line, not a new rule.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncFailureModesTest {

    private val server = MockWebServer()
    private val uncaught = mutableListOf<Throwable>()
    private val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val clock = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC)
    private val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    @Before
    fun setUp() {
        server.start()
        Thread.setDefaultUncaughtExceptionHandler { _, e -> synchronized(uncaught) { uncaught += e } }
    }

    @After
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        server.close()
        db.close()
    }

    // ---- Sign-in token ----

    @Test
    fun `token lookup offline (IOException)`() = assertSyncFailsSafely(token = { throw IOException("offline") })

    @Test
    fun `token lookup offline (Firebase ExecutionException)`() =
        assertSyncFailsSafely(token = { throw ExecutionException(RuntimeException("network error")) })

    @Test
    fun `token lookup throws something unexpected`() =
        assertSyncFailsSafely(token = { throw IllegalStateException("?") })

    @Test
    fun `no token (dead session)`() = assertSyncFailsSafely(token = { null }, server = status(401))

    // ---- Server and network ----

    @Test
    fun `server unreachable`() {
        server.close()
        assertSyncFailsSafely()
    }

    @Test
    fun `server error 500`() = assertSyncFailsSafely(server = status(500))

    @Test
    fun `server unavailable 503`() = assertSyncFailsSafely(server = status(503))

    @Test
    fun `garbage body`() = assertSyncFailsSafely(server = MockResponse.Builder().code(200).body("<html>wifi login</html>").build())

    @Test
    fun `empty body`() = assertSyncFailsSafely(server = MockResponse.Builder().code(200).body("").build())

    @Test
    fun `connection dropped before the response`() =
        assertSyncFailsSafely(server = MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())

    @Test
    fun `connection dropped mid-body`() = assertSyncFailsSafely(
        server = MockResponse.Builder().code(200).body("{\"results\":[".padEnd(4096, ' '))
            .onResponseBody(SocketEffect.CloseSocket()).build()
    )

    private fun status(code: Int) = MockResponse.Builder().code(code).body("{}").build()

    private fun assertSyncFailsSafely(
        token: AuthTokenProvider = AuthTokenProvider { "token" },
        server: MockResponse = status(200),
    ) = runBlocking {
        this@SyncFailureModesTest.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = server
        }
        val holder = DatabaseHolder.fixed(db)
        val exercise = SyncFixtures.exercise("Offline Curl")
        ExerciseRepositoryImpl(holder, OutboxRecorder(holder, clock), clock).createExercise(exercise)
        val client = OkHttpClient.Builder().connectTimeout(2, TimeUnit.SECONDS).readTimeout(2, TimeUnit.SECONDS).build()
        val apis = SyncApis.create(this@SyncFailureModesTest.server.url("/").toString(), token, client)
        val engine = SyncEngine(SyncLocalStore(holder, clock), apis)

        // SyncScheduler runs the cycle inside runCatching, so a thrown error is a handled failure.
        val result = withTimeout(10_000) { runCatching { engine.sync() } }

        assertTrue("sync should fail, got $result", result.isFailure)
        Thread.sleep(200) // give OkHttp's threads time to rethrow anything they caught
        assertEquals(emptyList<Throwable>(), synchronized(uncaught) { uncaught.toList() })
        assertEquals("the unsynced change must wait for the next try", 1, db.syncDao().outboxCount())
        assertNotNull(db.syncDao().getExerciseIncludingDeleted(exercise.id))
    }
}
