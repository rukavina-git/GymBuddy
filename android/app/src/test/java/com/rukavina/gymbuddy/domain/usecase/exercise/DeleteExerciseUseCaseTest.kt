package com.rukavina.gymbuddy.domain.usecase.exercise

import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.testutil.FakeExerciseRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteExerciseUseCaseTest {

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

    @Test
    fun `fails without deleting when id is blank`() = runBlocking {
        val repo = FakeExerciseRepository(listOf(exercise))
        val useCase = DeleteExerciseUseCase(repo)

        val result = useCase("  ")

        assertTrue(result.isFailure)
        assertEquals("Exercise ID must be valid", result.exceptionOrNull()?.message)
        assertEquals(exercise, repo.getExerciseById("ex-1"))
    }

    @Test
    fun `delegates to the repository when id is valid`() = runBlocking {
        val repo = FakeExerciseRepository(listOf(exercise))
        val useCase = DeleteExerciseUseCase(repo)

        val result = useCase("ex-1")

        assertTrue(result.isSuccess)
        assertNull(repo.getExerciseById("ex-1"))
    }
}
