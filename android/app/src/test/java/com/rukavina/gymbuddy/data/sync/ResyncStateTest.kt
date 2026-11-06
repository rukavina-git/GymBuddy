package com.rukavina.gymbuddy.data.sync

import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.sync.remote.SyncApis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Plan 7 step 11b: SyncEngine.resync through a 410 re-download (R17-C2a, R2). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ResyncStateTest {

    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private lateinit var db: AppDatabase
    private lateinit var store: SyncLocalStore
    private lateinit var engine: SyncEngine
    /** Pull responses in order (code to body); the last one repeats. */
    private val pulls = ArrayDeque<Pair<Int, String>>()
    private val seenDuringPage = mutableListOf<Resync>()
    private val owner = MutableStateFlow<String?>("uid-a")

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val holder = DatabaseHolder.fixed(db, owner = "uid-a")
        store = SyncLocalStore(holder, clock)
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val (code, body) = when {
                request.url.encodedPath.endsWith("/sync/pull") || request.url.encodedPath.contains("/sync/changes") ->
                    if (pulls.size > 1) pulls.removeFirst() else pulls.first()
                request.url.encodedPath.endsWith("/sync/push") -> 200 to """{"results":[]}"""
                else -> 500 to "{}"
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("x")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        engine = SyncEngine(
            store,
            SyncApis.create("http://canned.invalid/", { "token" }, client),
            pageObserver = { _, _ -> seenDuringPage += engine.rawResync.value },
            mutex = holder.lock,
            owner = owner,
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `a 410 runs, then idles once the full pull lands`() = runBlocking {
        pulls += 410 to "{}"
        pulls += 200 to OK
        engine.sync()
        assertEquals(listOf(Resync.Running), seenDuringPage)
        assertEquals(Resync.Idle, engine.rawResync.value)
    }

    @Test
    fun `a failed full pull is Failed until a later pull succeeds`() = runBlocking {
        pulls += 410 to "{}"
        pulls += 503 to "{}"
        pulls += 200 to OK
        assertTrue(runCatching { engine.sync() }.isFailure)
        assertEquals(Resync.Failed, engine.rawResync.value)

        engine.sync() // Retry
        assertEquals(listOf(Resync.Running), seenDuringPage)
        assertEquals(Resync.Idle, engine.rawResync.value)
    }

    @Test
    fun `a failed re-download reads Idle once another owner opens`() = runBlocking {
        pulls += 410 to "{}"
        pulls += 503 to "{}"
        runCatching { engine.sync() }
        assertEquals(Resync.Failed, engine.resync.first())

        owner.value = "guest-1" // sign out, guest or another account
        assertEquals(Resync.Idle, engine.resync.first())
        owner.value = "uid-a"
        assertEquals(Resync.Failed, engine.resync.first())
    }

    @Test
    fun `an ordinary pull stays Idle`() = runBlocking {
        pulls += 200 to OK
        engine.sync()
        assertEquals(listOf(Resync.Idle), seenDuringPage)
    }

    private companion object {
        const val OK = """{"nextCursor":"c1","hasMore":false}"""
    }
}
