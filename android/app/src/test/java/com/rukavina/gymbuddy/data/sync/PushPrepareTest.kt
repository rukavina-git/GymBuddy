package com.rukavina.gymbuddy.data.sync

import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.local.entity.OutboxEntryEntity
import com.rukavina.gymbuddy.data.local.entity.OutboxOperation
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.local.entity.SyncLogOutcome
import com.rukavina.gymbuddy.data.repository.ExerciseRepositoryImpl
import com.rukavina.gymbuddy.data.sync.remote.SyncApis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.rukavina.gymbuddy.data.remote.generated.models.MutationResult
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Push: outbox rows that can't be sent, and a batch the server answers only with ERROR. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PushPrepareTest {

    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private lateinit var db: AppDatabase
    private lateinit var holder: DatabaseHolder
    private lateinit var store: SyncLocalStore
    private lateinit var engine: SyncEngine
    private var pushBody = """{"results":[]}"""
    private var pushes = 0

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        holder = DatabaseHolder.fixed(db, owner = "uid-a")
        store = SyncLocalStore(holder, clock)
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val (code, body) = if (request.url.encodedPath.endsWith("/sync/push")) {
                pushes++
                200 to pushBody
            } else {
                500 to "{}"
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("x")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        engine = SyncEngine(
            store,
            SyncApis.create("http://canned.invalid/", { "token" }, client),
            mutex = holder.lock,
            owner = MutableStateFlow("uid-a"),
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `an outbox row whose entity is gone is dropped without a request`() = runBlocking {
        db.syncDao().putOutboxEntry(
            OutboxEntryEntity(SyncEntityType.EXERCISE, SyncFixtures.id(), OutboxOperation.UPSERT, 1, 0)
        )

        val summary = engine.push()

        assertEquals(0, summary.requests)
        assertEquals(0, summary.unsendable)
        assertEquals(0, db.syncDao().outboxCount())
        assertEquals(0, pushes)
    }

    @Test
    fun `a row with a non-UUID id is unsendable, logged and dropped`() = runBlocking {
        ExerciseRepositoryImpl(holder, OutboxRecorder(holder, clock), clock)
            .createExercise(SyncFixtures.exercise().copy(id = "not-a-uuid"))

        val summary = engine.push()

        assertEquals(1, summary.unsendable)
        assertEquals(0, db.syncDao().outboxCount())
        assertEquals(listOf(SyncLogOutcome.UNSENDABLE), db.syncDao().getLog().map { it.outcome })
        assertEquals(0, pushes)
    }

    @Test
    fun `a batch answered only with ERROR stops after one request`() = runBlocking {
        val exercise = SyncFixtures.exercise()
        ExerciseRepositoryImpl(holder, OutboxRecorder(holder, clock), clock).createExercise(exercise)
        pushBody = """{"results":[{"entityType":"EXERCISE","entityId":"${exercise.id}","status":"ERROR"}]}"""

        val summary = engine.push()

        assertEquals(1, summary.requests)
        assertEquals(1, summary.count(MutationResult.Status.ERROR))
        assertEquals(1, pushes)
        assertEquals(1, db.syncDao().outboxCount())
    }
}
