package com.rukavina.gymbuddy.domain.usecase.workout

import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.domain.model.PerformedExercise
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutSet
import com.rukavina.gymbuddy.testutil.FakeExerciseRepository
import com.rukavina.gymbuddy.testutil.FakeWorkoutSessionRepository
import com.rukavina.gymbuddy.testutil.FixedIdGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateWorkoutSessionUseCaseTest {

    private val exercise = Exercise(
        id = "ex-1",
        name = "Bench Press",
        primaryMuscles = listOf(MuscleGroup.CHEST),
        secondaryMuscles = emptyList(),
        difficulty = DifficultyLevel.INTERMEDIATE,
        equipmentNeeded = emptyList(),
        category = ExerciseCategory.STRENGTH,
        exerciseType = ExerciseType.COMPOUND,
        trackingType = ExerciseTrackingType.WEIGHT_REPS
    )

    private fun set(id: String, reps: Int? = 8) = WorkoutSet(
        id = id,
        reps = reps,
        weightKg = 50f,
        isCompleted = true,
        orderIndex = 0
    )

    private fun performedExercise(id: String, exerciseId: String = "ex-1") = PerformedExercise(
        id = id,
        exerciseId = exerciseId,
        orderIndex = 0,
        exerciseName = "",
        exerciseCategory = ExerciseCategory.CARDIO,
        exerciseTrackingType = ExerciseTrackingType.REPS_ONLY,
        exercisePrimaryMuscles = emptyList(),
        sets = listOf(set("set-$id"))
    )

    private fun session(
        id: String = "",
        startedAt: Long = 1_000L,
        durationSeconds: Int = 600,
        performedExercises: List<PerformedExercise> = listOf(performedExercise(id = ""))
    ) = WorkoutSession(
        id = id,
        startedAt = startedAt,
        durationSeconds = durationSeconds,
        title = "Workout",
        performedExercises = performedExercises
    )

    private fun buildUseCase(
        idGenerator: FixedIdGenerator = FixedIdGenerator("session-id", "pe-id"),
        exerciseRepo: FakeExerciseRepository = FakeExerciseRepository(listOf(exercise)),
        sessionRepo: FakeWorkoutSessionRepository = FakeWorkoutSessionRepository()
    ) = Triple(
        CreateWorkoutSessionUseCase(sessionRepo, idGenerator, ValidateWorkoutSessionSetsUseCase(exerciseRepo)),
        sessionRepo,
        idGenerator
    )

    @Test
    fun `generates ids for the session and every performed exercise when blank`() = runBlocking {
        val (useCase, sessionRepo, _) = buildUseCase()

        val result = useCase(session(id = "", performedExercises = listOf(performedExercise(id = ""))))

        assertTrue(result.isSuccess)
        val saved = sessionRepo.created.single()
        assertEquals("session-id", saved.id)
        assertEquals("pe-id", saved.performedExercises.single().id)
    }

    @Test
    fun `preserves ids that are already set instead of generating new ones`() = runBlocking {
        val (useCase, sessionRepo, idGenerator) = buildUseCase()

        val result = useCase(
            session(id = "existing-session", performedExercises = listOf(performedExercise(id = "existing-pe")))
        )

        assertTrue(result.isSuccess)
        val saved = sessionRepo.created.single()
        assertEquals("existing-session", saved.id)
        assertEquals("existing-pe", saved.performedExercises.single().id)
        assertEquals(0, idGenerator.callCount)
    }

    @Test
    fun `persists the snapshot-stamped session, not the pre-validation one`() = runBlocking {
        val (useCase, sessionRepo, _) = buildUseCase()

        useCase(session())

        val saved = sessionRepo.created.single()
        assertEquals("Bench Press", saved.performedExercises.single().exerciseName)
        assertEquals(ExerciseCategory.STRENGTH, saved.performedExercises.single().exerciseCategory)
    }

    @Test
    fun `fails without persisting when duration is negative`() = runBlocking {
        val (useCase, sessionRepo, _) = buildUseCase()

        val result = useCase(session(durationSeconds = -1))

        assertTrue(result.isFailure)
        assertEquals("Duration must be non-negative", result.exceptionOrNull()?.message)
        assertTrue(sessionRepo.created.isEmpty())
    }

    @Test
    fun `fails without persisting when startedAt is not positive`() = runBlocking {
        val (useCase, sessionRepo, _) = buildUseCase()

        val result = useCase(session(startedAt = 0L))

        assertTrue(result.isFailure)
        assertEquals("Invalid workout session start time", result.exceptionOrNull()?.message)
        assertTrue(sessionRepo.created.isEmpty())
    }

    @Test
    fun `fails without persisting when a referenced exercise cannot be found`() = runBlocking {
        val (useCase, sessionRepo, _) = buildUseCase(exerciseRepo = FakeExerciseRepository(emptyList()))

        val result = useCase(session())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(sessionRepo.created.isEmpty())
    }

    @Test
    fun `fails without persisting when a completed set is invalid for its tracking type`() = runBlocking {
        val (useCase, sessionRepo, _) = buildUseCase()

        val invalidSession = session(
            performedExercises = listOf(performedExercise(id = "pe-1").let {
                it.copy(sets = listOf(set("set-1", reps = null)))
            })
        )

        val result = useCase(invalidSession)

        assertTrue(result.isFailure)
        assertEquals("reps is required", result.exceptionOrNull()?.message)
        assertTrue(sessionRepo.created.isEmpty())
        assertNull(sessionRepo.getWorkoutSessionById("session-id"))
    }
}
