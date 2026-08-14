package com.rukavina.gymbuddy.domain.usecase.exercise

import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.testutil.FakeExerciseRepository
import com.rukavina.gymbuddy.testutil.FixedIdGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateExerciseUseCaseTest {

    private fun exercise(
        id: String = "",
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
    fun `generates an id when blank`() = runBlocking {
        val repo = FakeExerciseRepository()
        val idGenerator = FixedIdGenerator("new-exercise-id")
        val useCase = CreateExerciseUseCase(repo, idGenerator)

        val result = useCase(exercise(id = ""))

        assertTrue(result.isSuccess)
        assertEquals("new-exercise-id", repo.getExerciseById("new-exercise-id")?.id)
    }

    @Test
    fun `preserves an id that is already set`() = runBlocking {
        val repo = FakeExerciseRepository()
        val idGenerator = FixedIdGenerator("new-exercise-id")
        val useCase = CreateExerciseUseCase(repo, idGenerator)

        val result = useCase(exercise(id = "existing-id"))

        assertTrue(result.isSuccess)
        assertEquals(0, idGenerator.callCount)
        assertEquals("existing-id", repo.getExerciseById("existing-id")?.id)
    }

    @Test
    fun `fails without persisting when name is blank`() = runBlocking {
        val repo = FakeExerciseRepository()
        val useCase = CreateExerciseUseCase(repo, FixedIdGenerator())

        val result = useCase(exercise(name = "  "))

        assertTrue(result.isFailure)
        assertEquals("Exercise name cannot be blank", result.exceptionOrNull()?.message)
    }

    @Test
    fun `fails without persisting when there are no primary muscles`() = runBlocking {
        val repo = FakeExerciseRepository()
        val useCase = CreateExerciseUseCase(repo, FixedIdGenerator())

        val result = useCase(exercise(primaryMuscles = emptyList()))

        assertTrue(result.isFailure)
        assertEquals("Exercise must target at least one primary muscle", result.exceptionOrNull()?.message)
    }
}
