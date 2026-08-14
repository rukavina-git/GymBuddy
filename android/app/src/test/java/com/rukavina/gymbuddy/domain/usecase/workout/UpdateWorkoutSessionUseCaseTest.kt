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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateWorkoutSessionUseCaseTest {

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

    private fun performedExercise(reps: Int? = 8) = PerformedExercise(
        id = "pe-1",
        exerciseId = "ex-1",
        orderIndex = 0,
        exerciseName = "",
        exerciseCategory = ExerciseCategory.CARDIO,
        exerciseTrackingType = ExerciseTrackingType.REPS_ONLY,
        exercisePrimaryMuscles = emptyList(),
        sets = listOf(WorkoutSet(id = "set-1", reps = reps, weightKg = 50f, isCompleted = true, orderIndex = 0))
    )

    private fun session(
        id: String = "session-1",
        startedAt: Long = 1_000L,
        durationSeconds: Int = 600,
        performedExercises: List<PerformedExercise> = listOf(performedExercise())
    ) = WorkoutSession(
        id = id,
        startedAt = startedAt,
        durationSeconds = durationSeconds,
        title = "Workout",
        performedExercises = performedExercises
    )

    private fun buildUseCase(
        exerciseRepo: FakeExerciseRepository = FakeExerciseRepository(listOf(exercise)),
        sessionRepo: FakeWorkoutSessionRepository = FakeWorkoutSessionRepository()
    ) = UpdateWorkoutSessionUseCase(sessionRepo, ValidateWorkoutSessionSetsUseCase(exerciseRepo)) to sessionRepo

    @Test
    fun `persists the snapshot-stamped session on success`() = runBlocking {
        val (useCase, sessionRepo) = buildUseCase()

        val result = useCase(session())

        assertTrue(result.isSuccess)
        val saved = sessionRepo.updated.single()
        assertEquals("session-1", saved.id)
        assertEquals("Bench Press", saved.performedExercises.single().exerciseName)
    }

    @Test
    fun `fails without persisting when id is blank`() = runBlocking {
        val (useCase, sessionRepo) = buildUseCase()

        val result = useCase(session(id = ""))

        assertTrue(result.isFailure)
        assertEquals("Workout session ID cannot be blank", result.exceptionOrNull()?.message)
        assertTrue(sessionRepo.updated.isEmpty())
    }

    @Test
    fun `fails without persisting when duration is negative`() = runBlocking {
        val (useCase, sessionRepo) = buildUseCase()

        val result = useCase(session(durationSeconds = -5))

        assertTrue(result.isFailure)
        assertEquals("Duration must be non-negative", result.exceptionOrNull()?.message)
        assertTrue(sessionRepo.updated.isEmpty())
    }

    @Test
    fun `fails without persisting when startedAt is not positive`() = runBlocking {
        val (useCase, sessionRepo) = buildUseCase()

        val result = useCase(session(startedAt = -1L))

        assertTrue(result.isFailure)
        assertEquals("Invalid workout session start time", result.exceptionOrNull()?.message)
        assertTrue(sessionRepo.updated.isEmpty())
    }

    @Test
    fun `fails without persisting when a completed set is invalid`() = runBlocking {
        val (useCase, sessionRepo) = buildUseCase()

        val result = useCase(session(performedExercises = listOf(performedExercise(reps = 0))))

        assertTrue(result.isFailure)
        assertEquals("reps must be greater than zero", result.exceptionOrNull()?.message)
        assertTrue(sessionRepo.updated.isEmpty())
    }

    @Test
    fun `fails without persisting when a referenced exercise cannot be found`() = runBlocking {
        val (useCase, sessionRepo) = buildUseCase(exerciseRepo = FakeExerciseRepository(emptyList()))

        val result = useCase(session())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(sessionRepo.updated.isEmpty())
    }
}
