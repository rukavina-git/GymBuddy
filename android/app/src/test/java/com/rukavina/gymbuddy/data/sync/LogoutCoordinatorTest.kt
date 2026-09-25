package com.rukavina.gymbuddy.data.sync

import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.repository.ExerciseRepositoryImpl
import com.rukavina.gymbuddy.data.repository.WorkoutSessionRepositoryImpl
import com.rukavina.gymbuddy.data.sync.remote.SyncApis
import com.rukavina.gymbuddy.domain.model.SyncState
import com.rukavina.gymbuddy.ui.settings.unsyncedChangeLines
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        val coordinator = LogoutCoordinator(store, { flushes++; true }, auth)

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
        val coordinator = LogoutCoordinator(store, { engine.flushOutbox() }, auth)

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

        LogoutCoordinator(store, { true }, auth).logOutAndDelete()

        assertEquals(1, db.exerciseDao().getDefaultExerciseCount())
        assertNull(db.syncDao().getExerciseIncludingDeleted(custom.id))
        assertTrue(store.log().isEmpty())
        assertNotNull(db.exerciseDao())
        assertFalse(auth.currentUid != null)
    }
}
