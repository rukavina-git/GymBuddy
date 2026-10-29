package com.rukavina.gymbuddy.data.sync

import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.repository.ExerciseRepositoryImpl
import com.rukavina.gymbuddy.data.repository.WorkoutSessionRepositoryImpl
import com.rukavina.gymbuddy.data.sync.remote.SyncApis
import com.rukavina.gymbuddy.domain.model.SyncState
import com.rukavina.gymbuddy.ui.settings.unsyncedChangeLines
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
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
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Records sign-outs instead of talking to Firebase. */
class FakeAuthSession(override var currentUid: String? = "uid-1") : AuthSession {
    var signOuts = 0
    override suspend fun idToken(): String? = currentUid?.let { "token" }
    override fun signOut() {
        signOuts++
        currentUid = null
    }
}

/**
 * Scenario 12, the two logout outcomes that need no server: an empty
 * outbox (immediate logout) and a failed flush (warning, nothing
 * deleted until confirmed). The successful-flush outcome runs against
 * the real backend in e2e/LogoutE2eTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LogoutCoordinatorTest {

    private lateinit var db: AppDatabase
    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private lateinit var store: SyncLocalStore
    private lateinit var sessions: WorkoutSessionRepositoryImpl
    private lateinit var exercises: ExerciseRepositoryImpl
    private val auth = FakeAuthSession()
    private val direct: suspend (suspend () -> Unit) -> Unit = { block -> block() }

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(org.robolectric.RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = SyncLocalStore(db, clock)
        val outbox = OutboxRecorder(db, clock)
        sessions = WorkoutSessionRepositoryImpl(db.workoutSessionDao(), outbox, clock)
        exercises = ExerciseRepositoryImpl(db.exerciseDao(), db.userExerciseStateDao(), outbox, clock)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `empty outbox logs out immediately without a flush and wipes local user data`() = runBlocking {
        val session = SyncFixtures.session()
        sessions.createWorkoutSession(session)
        db.syncDao().wipeOutbox() // as if it had already been pushed
        var flushes = 0
        val coordinator = LogoutCoordinator(store, { flushes++; true }, auth, direct)

        val result = coordinator.requestLogout()

        assertEquals(LogoutResult.LoggedOut, result)
        assertEquals("no network needed when nothing is pending", 0, flushes)
        assertEquals(1, auth.signOuts)
        assertNull(db.syncDao().getSessionIncludingDeleted(session.id))
    }

    @Test
    fun `failed flush warns with pending changes grouped by type and deletes nothing until confirmed`() = runBlocking {
        val first = SyncFixtures.session()
        val second = SyncFixtures.session()
        val exercise = SyncFixtures.exercise()
        sessions.createWorkoutSession(first)
        sessions.createWorkoutSession(second)
        exercises.createExercise(exercise)
        // A real engine pointed at a port nothing listens on: the flush fails like it would offline.
        val offline = SyncApis.create("http://127.0.0.1:9/", { "token" })
        val engine = SyncEngine(store, offline)
        val coordinator = LogoutCoordinator(store, { engine.flushOutbox() }, auth, { block -> engine.exclusive(block) })

        val result = coordinator.requestLogout()

        assertEquals(
            LogoutResult.UnsyncedChanges(mapOf(SyncEntityType.WORKOUT_SESSION to 2, SyncEntityType.EXERCISE to 1)),
            result
        )
        assertEquals(listOf("2 workout sessions", "1 custom exercise"), unsyncedChangeLines((result as LogoutResult.UnsyncedChanges).pendingByType))
        assertEquals("still signed in", 0, auth.signOuts)
        assertEquals(3, db.syncDao().outboxCount())
        assertEquals(SyncState.PENDING, db.syncDao().getSessionIncludingDeleted(first.id)!!.syncState)

        // The user taps "Log out and delete".
        coordinator.logOutAndDelete()

        assertEquals(1, auth.signOuts)
        assertEquals(0, db.syncDao().outboxCount())
        assertNull(db.syncDao().getSessionIncludingDeleted(first.id))
        assertNull(db.syncDao().getExerciseIncludingDeleted(exercise.id))
    }

    @Test
    fun `logout keeps the DEFAULT reference library`() = runBlocking {
        exercises.createExercise(SyncFixtures.exercise().copy(source = com.rukavina.gymbuddy.domain.model.EntitySource.DEFAULT))
        val custom = SyncFixtures.exercise()
        exercises.createExercise(custom)

        LogoutCoordinator(store, { true }, auth, direct).logOutAndDelete()

        assertEquals(1, db.exerciseDao().getDefaultExerciseCount())
        assertNull(db.syncDao().getExerciseIncludingDeleted(custom.id))
        assertTrue(store.log().isEmpty())
        assertNotNull(db.exerciseDao())
        assertFalse(auth.currentUid != null)
    }

    // ---- logout under the sync lock ----

    /** No network: every pull returns one empty page with cursor "c1"; everything else is a 500. */
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
    fun `logout wipes before signing out`() = runBlocking {
        val order = mutableListOf<String>()
        sessions.createWorkoutSession(SyncFixtures.session())
        val auth = object : AuthSession {
            override val currentUid: String? = "uid-1"
            override suspend fun idToken(): String? = null
            override fun signOut() {
                order += "signOut:outbox=${runBlocking { store.outboxCount() }}"
            }
        }

        LogoutCoordinator(store, { true }, auth, direct).logOutAndDelete()

        assertEquals(listOf("signOut:outbox=0"), order)
    }

    @Test
    fun `a failed wipe does not sign out`() {
        val session = SyncFixtures.session()
        runBlocking { sessions.createWorkoutSession(session) }
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_wipe BEFORE DELETE ON workout_sessions BEGIN SELECT RAISE(ABORT, 'disk full'); END"
        )
        val coordinator = LogoutCoordinator(store, { true }, auth, direct)

        assertThrows(Exception::class.java) { runBlocking { coordinator.logOutAndDelete() } }
        assertEquals(0, auth.signOuts)
        assertNotNull("data kept for the still-signed-in user", runBlocking { db.syncDao().getSessionIncludingDeleted(session.id) })
    }

    @Test
    fun `logout waits for a running cycle and nothing is written after the wipe`() = runBlocking {
        val session = SyncFixtures.session()
        sessions.createWorkoutSession(session)
        db.syncDao().wipeOutbox()
        val inPage = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val engine = SyncEngine(
            store, cannedApis(),
            pageObserver = PullPageObserver { _, _ -> inPage.complete(Unit); release.await() },
            isSignedIn = { auth.currentUid != null }
        )
        val coordinator = LogoutCoordinator(store, { engine.flushOutbox() }, auth, { block -> engine.exclusive(block) })

        val cycle = async(Dispatchers.Default) { engine.sync() }
        inPage.await() // the cycle is inside its pull page, cursor not yet committed
        val logout = async(Dispatchers.Default) { coordinator.logOutAndDelete() }
        delay(200)
        assertFalse("logout waits for the running cycle", logout.isCompleted)
        assertEquals(0, auth.signOuts)

        release.complete(Unit)
        cycle.await()
        logout.await()

        assertEquals(1, auth.signOuts)
        assertNull("the page's cursor was wiped, not committed after", store.cursor())
        assertNull(db.syncDao().getSessionIncludingDeleted(session.id))
    }

    @Test
    fun `a cycle queued behind logout does nothing once signed out`() = runBlocking {
        db.syncDao().wipeOutbox()
        var pulls = 0
        val engine = SyncEngine(
            store, cannedApis(),
            pageObserver = PullPageObserver { _, _ -> pulls++ },
            isSignedIn = { auth.currentUid != null }
        )
        val coordinator = LogoutCoordinator(store, { engine.flushOutbox() }, auth, { block -> engine.exclusive(block) })
        val wiping = CompletableDeferred<Unit>()
        val finishWipe = CompletableDeferred<Unit>()

        val logout = async(Dispatchers.Default) {
            engine.exclusive { wiping.complete(Unit); finishWipe.await() }
            coordinator.logOutAndDelete()
        }
        wiping.await()
        val cycle = async(Dispatchers.Default) { engine.sync() } // queued on the lock
        finishWipe.complete(Unit)
        logout.await()
        val summary = cycle.await()

        assertEquals(0, pulls)
        assertEquals(0, summary.pull.pages)
        assertNull(store.cursor())
    }
}
