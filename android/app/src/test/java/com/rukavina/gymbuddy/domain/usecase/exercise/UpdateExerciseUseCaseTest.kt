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
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateExerciseUseCaseTest {

    private fun exercise(
        id: String = "ex-1",
        name: String = "Bench Press",
        primaryMuscles: List<MuscleGroup> = listOf(MuscleGroup.CHEST)
    ) = Exercise(
        id = id,
        name = name,
        primaryMuscles = primaryMuscles,
        secondaryMuscles = emptyList(),
        difficulty = DifficultyLevel.INTERMEDIATE,
        equipmentNeeded = emptyList(),
        category = ExerciseCategory.STRENGTH,
        exerciseType = ExerciseType.COMPOUND,
        trackingType = ExerciseTrackingType.WEIGHT_REPS
    )

    @Test
    fun `updates the exercise on success`() = runBlocking {
        val repo = FakeExerciseRepository(listOf(exercise()))
        val useCase = UpdateExerciseUseCase(repo)

        val result = useCase(exercise(name = "Incline Bench Press"))

        assertTrue(result.isSuccess)
        assertEquals("Incline Bench Press", repo.getExerciseById("ex-1")?.name)
    }

    @Test
    fun `fails without persisting when id is blank`() = runBlocking {
        val repo = FakeExerciseRepository(listOf(exercise()))
        val useCase = UpdateExerciseUseCase(repo)

        val result = useCase(exercise(id = ""))

        assertTrue(result.isFailure)
        assertEquals("Exercise ID must be valid", result.exceptionOrNull()?.message)
        assertEquals("Bench Press", repo.getExerciseById("ex-1")?.name)
    }

    @Test
    fun `fails without persisting when name is blank`() = runBlocking {
        val repo = FakeExerciseRepository(listOf(exercise()))
        val useCase = UpdateExerciseUseCase(repo)

        val result = useCase(exercise(name = ""))

        assertTrue(result.isFailure)
        assertEquals("Exercise name cannot be blank", result.exceptionOrNull()?.message)
    }

    @Test
    fun `fails without persisting when there are no primary muscles`() = runBlocking {
        val repo = FakeExerciseRepository(listOf(exercise()))
        val useCase = UpdateExerciseUseCase(repo)

        val result = useCase(exercise(primaryMuscles = emptyList()))

        assertTrue(result.isFailure)
        assertEquals("Exercise must target at least one primary muscle", result.exceptionOrNull()?.message)
    }
}
