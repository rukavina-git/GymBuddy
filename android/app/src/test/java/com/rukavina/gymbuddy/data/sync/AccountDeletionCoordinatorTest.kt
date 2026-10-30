package com.rukavina.gymbuddy.data.sync

import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.repository.ExerciseRepositoryImpl
import com.rukavina.gymbuddy.data.repository.WorkoutSessionRepositoryImpl
import com.rukavina.gymbuddy.data.sync.remote.SyncApis
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.utils.ImageStorageUtil
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Account deletion over in-memory Room with a fake server call: nothing
 * local is deleted unless the server answered 204.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountDeletionCoordinatorTest {

    private lateinit var db: AppDatabase
    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private lateinit var store: SyncLocalStore
    private lateinit var sessions: WorkoutSessionRepositoryImpl
    private lateinit var exercises: ExerciseRepositoryImpl
    private val auth = FakeAuthSession()
    private val direct: suspend (suspend () -> Unit) -> Unit = { block -> block() }
    private val context get() = RuntimeEnvironment.getApplication()
    private val imagesDir get() = File(context.filesDir, "profile_images")

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = SyncLocalStore(db, clock)
        val outbox = OutboxRecorder(db, clock)
        sessions = WorkoutSessionRepositoryImpl(db.workoutSessionDao(), outbox, clock)
        exercises = ExerciseRepositoryImpl(db.exerciseDao(), db.userExerciseStateDao(), outbox, clock)
        imagesDir.mkdirs()
        File(imagesDir, "profile_1.jpg").writeText("jpeg")
    }

    @After
    fun tearDown() {
        db.close()
        imagesDir.deleteRecursively()
    }

    private fun coordinator(
        deleteOnServer: suspend () -> Int,
        runExclusive: suspend (suspend () -> Unit) -> Unit = direct
    ) = AccountDeletionCoordinator(
        store,
        deleteOnServer,
        deleteProfileImages = { ImageStorageUtil.deleteAllProfileImages(context) },
        auth = auth,
        runExclusive = runExclusive
    )

    @Test
    fun `204 wipes user data, outbox and cursor, keeps reference data, signs out, deletes images`() = runBlocking {
        val session = SyncFixtures.session()
        sessions.createWorkoutSession(session)
        exercises.createExercise(SyncFixtures.exercise().copy(source = EntitySource.DEFAULT))
        store.setCursor("c1")
        var calls = 0

        val result = coordinator({ calls++; 204 }).deleteAccount()

        assertEquals(AccountDeletionResult.Deleted, result)
        assertEquals(1, calls)
        assertNull(db.syncDao().getSessionIncludingDeleted(session.id))
        assertEquals(0, db.syncDao().outboxCount())
        assertNull(store.cursor())
        assertEquals("reference library kept", 1, db.exerciseDao().getDefaultExerciseCount())
        assertEquals(1, auth.signOuts)
        assertFalse(imagesDir.exists())
    }

    private suspend fun assertNothingDeleted(sessionId: String) {
        assertNotNull(db.syncDao().getSessionIncludingDeleted(sessionId))
        assertEquals(1, db.syncDao().outboxCount())
        assertEquals(0, auth.signOuts)
        assertTrue(imagesDir.exists())
    }

    @Test
    fun `network error deletes nothing and is retryable`() = runBlocking {
        val session = SyncFixtures.session()
        sessions.createWorkoutSession(session)

        val result = coordinator({ throw IOException("offline") }).deleteAccount()

        assertEquals(AccountDeletionResult.Retryable, result)
        assertNothingDeleted(session.id)
    }

    @Test
    fun `500 deletes nothing and is retryable`() = runBlocking {
        val session = SyncFixtures.session()
        sessions.createWorkoutSession(session)

        val result = coordinator({ 500 }).deleteAccount()

        assertEquals(AccountDeletionResult.Retryable, result)
        assertNothingDeleted(session.id)
    }

    @Test
    fun `401 deletes nothing and asks to sign in again`() = runBlocking {
        val session = SyncFixtures.session()
        sessions.createWorkoutSession(session)

        val result = coordinator({ 401 }).deleteAccount()

        assertEquals(AccountDeletionResult.SignInAgain, result)
        assertNothingDeleted(session.id)
    }

    @Test
    fun `a failed wipe after 204 keeps the user signed in, and retry only wipes`() {
        val session = SyncFixtures.session()
        runBlocking { sessions.createWorkoutSession(session) }
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_wipe BEFORE DELETE ON workout_sessions BEGIN SELECT RAISE(ABORT, 'disk full'); END"
        )
        val statuses = ArrayDeque(listOf(204, 401)) // the account is gone after the first call
        val coordinator = coordinator({ statuses.removeFirst() })

        assertThrows(Exception::class.java) { runBlocking { coordinator.deleteAccount() } }
        assertEquals(0, auth.signOuts)

        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_wipe")
        val result = runBlocking { coordinator.deleteAccount() }

        assertEquals(AccountDeletionResult.Deleted, result)
        assertEquals("server not called again", 1, statuses.size)
        assertEquals(1, auth.signOuts)
        assertNull(runBlocking { db.syncDao().getSessionIncludingDeleted(session.id) })
    }

    @Test
    fun `a second account deleted later in the same process still calls the server`() = runBlocking {
        var calls = 0
        val coordinator = coordinator({ calls++; 204 })
        coordinator.deleteAccount()
        auth.currentUid = "uid-2" // someone else signs in

        coordinator.deleteAccount()

        assertEquals(2, calls)
        assertEquals(2, auth.signOuts)
    }

    /** No network: every pull returns one page with cursor "c1"; everything else is a 500. */
    private fun cannedApis(): SyncApis {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val pull = chain.request().url.encodedPath.endsWith("/sync/pull")
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(if (pull) 200 else 500)
                .message("canned")
                .body((if (pull) """{"nextCursor":"c1","hasMore":false}""" else "{}").toResponseBody("application/json".toMediaType()))
                .build()
        }.build()
        return SyncApis.create("http://canned.invalid/", { "token" }, client)
    }

    @Test
    fun `a cycle queued behind the delete does nothing afterwards`() = runBlocking {
        var pulls = 0
        val engine = SyncEngine(
            store, cannedApis(),
            pageObserver = PullPageObserver { _, _ -> pulls++ },
            isSignedIn = { auth.currentUid != null }
        )
        val deleted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        // Holds the lock after the delete and wipe until the cycle is queued behind it.
        val coordinator = coordinator({ 204 }) { block ->
            engine.exclusive { block(); deleted.complete(Unit); release.await() }
        }

        val delete = async(Dispatchers.Default) { coordinator.deleteAccount() }
        deleted.await()
        // UNDISPATCHED runs sync() until its first suspension: waiting on the held lock.
        val cycle = async(start = CoroutineStart.UNDISPATCHED) { engine.sync() }
        assertFalse("the cycle is queued behind the delete", cycle.isCompleted)
        release.complete(Unit)
        assertEquals(AccountDeletionResult.Deleted, delete.await())
        val summary = cycle.await()

        assertEquals(0, pulls)
        assertEquals(0, summary.pull.pages)
        assertNull(store.cursor())
    }
}
