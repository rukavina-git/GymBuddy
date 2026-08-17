package com.rukavina.gymbuddy.data.repository

import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PerformedExercise
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutSet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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

/**
 * WorkoutSessionRepositoryImpl is thin, but it's the one place that stamps
 * updatedAt from the injected Clock and turns deletes into tombstones - the
 * two things worth a real Room database to verify, rather than a fake.
 *
 * Robolectric-run, unavoidably: Room.inMemoryDatabaseBuilder needs a real
 * Context, which only Robolectric (or an instrumented androidTest, not run
 * in this project's CI) can provide without a mocking framework. These
 * tests execute and pass for real, but - per ActiveWorkoutViewModelTest's
 * file comment - contribute nothing to the JaCoCo coverage number, since
 * the coverage agent can't see classes loaded through Robolectric's own
 * classloader. A known, accepted gap for the whole data/repository package.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkoutSessionRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: WorkoutSessionRepositoryImpl
    private val fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(org.robolectric.RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutSessionRepositoryImpl(db.workoutSessionDao(), fixedClock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun performedExercise(id: String, exerciseId: String = "ex-1") = PerformedExercise(
        id = id,
        exerciseId = exerciseId,
        orderIndex = 0,
        exerciseName = "Bench Press",
        exerciseCategory = ExerciseCategory.STRENGTH,
        exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
        exercisePrimaryMuscles = emptyList(),
        sets = listOf(WorkoutSet(id = "set-$id", weightKg = 60f, reps = 8, isCompleted = true, orderIndex = 0))
    )

    private fun session(
        id: String,
        startedAt: Long = 1_000L,
        title: String = "Push Day",
        performedExercises: List<PerformedExercise> = listOf(performedExercise("pe-$id"))
    ) = WorkoutSession(
        id = id,
        startedAt = startedAt,
        durationSeconds = 600,
        title = title,
        performedExercises = performedExercises
    )

    @Test
    fun `createWorkoutSession stamps updatedAt from the injected Clock, not the system clock`() = runBlocking {
        repository.createWorkoutSession(session("session-1"))

        val read = repository.getWorkoutSessionById("session-1")
        assertEquals(fixedClock.millis(), read!!.updatedAt)
        assertTrue(
            "updatedAt must come from the injected Clock, not System.currentTimeMillis()",
            kotlin.math.abs(read.updatedAt - System.currentTimeMillis()) > 1000L
        )
    }

    @Test
    fun `createWorkoutSession persists a session that reads back with its performed exercises and sets`() = runBlocking {
        repository.createWorkoutSession(session("session-1"))

        val read = repository.getWorkoutSessionById("session-1")!!
        assertEquals("Push Day", read.title)
        assertEquals(1, read.performedExercises.size)
        assertEquals(60f, read.performedExercises.single().sets.single().weightKg)
    }

    @Test
    fun `deleteWorkoutSession tombstones instead of removing the row, excluded from subsequent reads`() = runBlocking {
        repository.createWorkoutSession(session("session-1"))

        repository.deleteWorkoutSession("session-1")

        assertNull("a tombstoned session must not resolve by id", repository.getWorkoutSessionById("session-1"))
        val all = repository.getAllWorkoutSessions().first()
        assertTrue("a tombstoned session must not appear in the list query", all.none { it.id == "session-1" })
    }

    @Test
    fun `deleteWorkoutSession tombstones the row in place rather than removing it`() = runBlocking {
        repository.createWorkoutSession(session("session-1"))

        repository.deleteWorkoutSession("session-1")

        // No DAO method exposes deleted rows (by design - see WorkoutSessionDao's
        // class doc) so query the raw table directly to confirm this was an
        // UPDATE, not a DELETE, and that both timestamps came from the Clock.
        db.openHelper.readableDatabase.query(
            "SELECT deletedAt, updatedAt FROM workout_sessions WHERE id = 'session-1'"
        ).use { cursor ->
            assertTrue("the row must still exist after a tombstone delete", cursor.moveToFirst())
            assertEquals(fixedClock.millis(), cursor.getLong(0))
            assertEquals(fixedClock.millis(), cursor.getLong(1))
        }
    }

    @Test
    fun `getAllWorkoutSessions excludes deleted sessions but keeps other live sessions`() = runBlocking {
        repository.createWorkoutSession(session("session-1", startedAt = 1_000L))
        repository.createWorkoutSession(session("session-2", startedAt = 2_000L))

        repository.deleteWorkoutSession("session-1")

        val all = repository.getAllWorkoutSessions().first()
        assertEquals(listOf("session-2"), all.map { it.id })
    }

    @Test
    fun `updateWorkoutSession replaces performed exercises and re-stamps updatedAt`() = runBlocking {
        repository.createWorkoutSession(session("session-1"))

        val laterClock = Clock.fixed(fixedClock.instant().plusSeconds(3600), ZoneOffset.UTC)
        val laterRepository = WorkoutSessionRepositoryImpl(db.workoutSessionDao(), laterClock)
        val updated = session(
            id = "session-1",
            title = "Push Day (edited)",
            performedExercises = listOf(performedExercise("pe-new", exerciseId = "ex-2"))
        )
        laterRepository.updateWorkoutSession(updated)

        val read = repository.getWorkoutSessionById("session-1")!!
        assertEquals("Push Day (edited)", read.title)
        assertEquals(1, read.performedExercises.size)
        assertEquals("ex-2", read.performedExercises.single().exerciseId)
        assertEquals(laterClock.millis(), read.updatedAt)
    }

    @Test(expected = IllegalStateException::class)
    fun `createWorkoutSession refuses to persist a performed exercise with an unstamped snapshot`() = runBlocking {
        val unstamped = performedExercise("pe-1").copy(exerciseName = "")

        repository.createWorkoutSession(session("session-1", performedExercises = listOf(unstamped)))
        Unit
    }
}
