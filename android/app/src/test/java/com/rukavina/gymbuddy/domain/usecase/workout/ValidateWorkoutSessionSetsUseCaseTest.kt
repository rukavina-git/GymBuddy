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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ValidateWorkoutSessionSetsUseCaseTest {

    private fun exercise(
        id: String = "exercise-1",
        name: String = "Bench Press",
        trackingType: ExerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
        category: ExerciseCategory = ExerciseCategory.STRENGTH,
        primaryMuscles: List<MuscleGroup> = listOf(MuscleGroup.CHEST)
    ) = Exercise(
        id = id,
        name = name,
        primaryMuscles = primaryMuscles,
        secondaryMuscles = emptyList(),
        difficulty = DifficultyLevel.INTERMEDIATE,
        equipmentNeeded = emptyList(),
        category = category,
        exerciseType = ExerciseType.COMPOUND,
        trackingType = trackingType
    )

    private fun set(
        id: String = "set-1",
        weightKg: Float? = null,
        reps: Int? = null,
        durationSeconds: Int? = null,
        distanceMeters: Float? = null,
        isCompleted: Boolean = true,
        orderIndex: Int = 0
    ) = WorkoutSet(
        id = id,
        weightKg = weightKg,
        reps = reps,
        durationSeconds = durationSeconds,
        distanceMeters = distanceMeters,
        isCompleted = isCompleted,
        orderIndex = orderIndex
    )

    // Placeholder snapshot values - the field under test. A real caller
    // never has correct values here yet; that's exactly what the use
    // case is responsible for filling in.
    private fun performedExercise(
        id: String = "performed-1",
        exerciseId: String = "exercise-1",
        orderIndex: Int = 0,
        sets: List<WorkoutSet> = listOf(set())
    ) = PerformedExercise(
        id = id,
        exerciseId = exerciseId,
        orderIndex = orderIndex,
        exerciseName = "unstamped",
        exerciseCategory = ExerciseCategory.CARDIO,
        exerciseTrackingType = ExerciseTrackingType.REPS_ONLY,
        exercisePrimaryMuscles = emptyList(),
        sets = sets
    )

    private fun session(performedExercises: List<PerformedExercise>) = WorkoutSession(
        id = "session-1",
        startedAt = 1_000L,
        durationSeconds = 600,
        title = "Workout",
        performedExercises = performedExercises
    )

    @Test
    fun `stamps snapshot fields from the resolved exercise, overwriting whatever was passed in`() = runBlocking {
        val ex = exercise(
            id = "ex-1",
            name = "Barbell Squat",
            category = ExerciseCategory.STRENGTH,
            trackingType = ExerciseTrackingType.WEIGHT_REPS,
            primaryMuscles = listOf(MuscleGroup.LEGS, MuscleGroup.CORE)
        )
        val useCase = ValidateWorkoutSessionSetsUseCase(FakeExerciseRepository(listOf(ex)))

        val input = session(
            listOf(performedExercise(exerciseId = "ex-1", sets = listOf(set(reps = 8, weightKg = 100f))))
        )

        val result = useCase(input)

        val stamped = result.performedExercises.single()
        assertEquals("Barbell Squat", stamped.exerciseName)
        assertEquals(ExerciseCategory.STRENGTH, stamped.exerciseCategory)
        assertEquals(ExerciseTrackingType.WEIGHT_REPS, stamped.exerciseTrackingType)
        assertEquals(listOf(MuscleGroup.LEGS, MuscleGroup.CORE), stamped.exercisePrimaryMuscles)
    }

    @Test
    fun `resolves each performed exercise independently by its own exerciseId`() = runBlocking {
        val squat = exercise(id = "ex-squat", name = "Squat")
        val row = exercise(id = "ex-row", name = "Barbell Row")
        val useCase = ValidateWorkoutSessionSetsUseCase(FakeExerciseRepository(listOf(squat, row)))

        val input = session(
            listOf(
                performedExercise(id = "pe-1", exerciseId = "ex-squat", orderIndex = 0, sets = listOf(set(reps = 5))),
                performedExercise(id = "pe-2", exerciseId = "ex-row", orderIndex = 1, sets = listOf(set(reps = 10)))
            )
        )

        val result = useCase(input)

        assertEquals("Squat", result.performedExercises[0].exerciseName)
        assertEquals("Barbell Row", result.performedExercises[1].exerciseName)
    }

    @Test
    fun `passes validation using the resolved exercise's tracking type - duration`() = runBlocking {
        val ex = exercise(id = "ex-1", trackingType = ExerciseTrackingType.DURATION)
        val useCase = ValidateWorkoutSessionSetsUseCase(FakeExerciseRepository(listOf(ex)))

        val input = session(
            listOf(performedExercise(exerciseId = "ex-1", sets = listOf(set(durationSeconds = 45))))
        )

        val result = useCase(input)

        assertEquals(ExerciseTrackingType.DURATION, result.performedExercises.single().exerciseTrackingType)
    }

    @Test
    fun `fails validation using the resolved exercise's tracking type - duration required but missing`() {
        val ex = exercise(id = "ex-1", trackingType = ExerciseTrackingType.DURATION)
        val useCase = ValidateWorkoutSessionSetsUseCase(FakeExerciseRepository(listOf(ex)))

        val input = session(
            listOf(performedExercise(exerciseId = "ex-1", sets = listOf(set(durationSeconds = null))))
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(input) }
        }
        assertEquals("durationSeconds is required", exception.message)
    }

    @Test
    fun `fails validation using the resolved exercise's tracking type - weight_distance requires distance`() {
        val ex = exercise(id = "ex-1", trackingType = ExerciseTrackingType.WEIGHT_DISTANCE)
        val useCase = ValidateWorkoutSessionSetsUseCase(FakeExerciseRepository(listOf(ex)))

        val input = session(
            listOf(performedExercise(exerciseId = "ex-1", sets = listOf(set(distanceMeters = null, weightKg = 20f))))
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(input) }
        }
        assertEquals("distanceMeters is required", exception.message)
    }

    @Test
    fun `throws when the referenced exercise cannot be found`() {
        val useCase = ValidateWorkoutSessionSetsUseCase(FakeExerciseRepository(emptyList()))

        val input = session(listOf(performedExercise(exerciseId = "missing-exercise")))

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(input) }
        }
        assertEquals("Exercise missing-exercise not found", exception.message)
    }

    @Test
    fun `incomplete sets skip validation regardless of how invalid their values are`() = runBlocking {
        val ex = exercise(id = "ex-1", trackingType = ExerciseTrackingType.WEIGHT_REPS)
        val useCase = ValidateWorkoutSessionSetsUseCase(FakeExerciseRepository(listOf(ex)))

        val incompleteInvalidSet = set(id = "set-incomplete", reps = null, weightKg = -50f, isCompleted = false)
        val input = session(
            listOf(performedExercise(exerciseId = "ex-1", sets = listOf(incompleteInvalidSet)))
        )

        // Must not throw despite negative weight and missing reps, since
        // the set was never marked completed.
        val result = useCase(input)

        assertEquals("Bench Press", result.performedExercises.single().exerciseName)
    }

    @Test
    fun `a single invalid completed set fails the whole session even alongside valid sets`() {
        val ex = exercise(id = "ex-1", trackingType = ExerciseTrackingType.WEIGHT_REPS)
        val useCase = ValidateWorkoutSessionSetsUseCase(FakeExerciseRepository(listOf(ex)))

        val input = session(
            listOf(
                performedExercise(
                    exerciseId = "ex-1",
                    sets = listOf(
                        set(id = "set-1", reps = 10, orderIndex = 0),
                        set(id = "set-2", reps = 0, orderIndex = 1)
                    )
                )
            )
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(input) }
        }
    }
}
