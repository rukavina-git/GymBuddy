package com.rukavina.gymbuddy.domain.usecase.template

import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.domain.model.TemplateExercise
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.testutil.FakeExerciseRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StampTemplateExerciseSnapshotsUseCaseTest {

    private fun exercise(
        id: String = "exercise-1",
        name: String = "Bench Press",
        trackingType: ExerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS
    ) = Exercise(
        id = id,
        name = name,
        primaryMuscles = listOf(MuscleGroup.CHEST),
        secondaryMuscles = emptyList(),
        difficulty = DifficultyLevel.INTERMEDIATE,
        equipmentNeeded = emptyList(),
        category = ExerciseCategory.STRENGTH,
        exerciseType = ExerciseType.COMPOUND,
        trackingType = trackingType
    )

    // Placeholder snapshot values - what the use case must overwrite.
    private fun templateExercise(
        id: String = "te-1",
        exerciseId: String = "exercise-1",
        orderIndex: Int = 0,
        plannedSets: Int = 3
    ) = TemplateExercise(
        id = id,
        exerciseId = exerciseId,
        exerciseName = "unstamped",
        exerciseTrackingType = ExerciseTrackingType.REPS_ONLY,
        plannedSets = plannedSets,
        orderIndex = orderIndex
    )

    private fun template(templateExercises: List<TemplateExercise>) = WorkoutTemplate(
        id = "template-1",
        title = "Push Day",
        templateExercises = templateExercises
    )

    @Test
    fun `stamps exerciseName and exerciseTrackingType from the resolved exercise`() = runBlocking {
        val ex = exercise(id = "ex-1", name = "Overhead Press", trackingType = ExerciseTrackingType.WEIGHT_REPS)
        val useCase = StampTemplateExerciseSnapshotsUseCase(FakeExerciseRepository(listOf(ex)))

        val input = template(listOf(templateExercise(exerciseId = "ex-1")))

        val result = useCase(input)

        val stamped = result.templateExercises.single()
        assertEquals("Overhead Press", stamped.exerciseName)
        assertEquals(ExerciseTrackingType.WEIGHT_REPS, stamped.exerciseTrackingType)
    }

    @Test
    fun `resolves each template exercise independently by its own exerciseId`() = runBlocking {
        val squat = exercise(id = "ex-squat", name = "Squat", trackingType = ExerciseTrackingType.WEIGHT_REPS)
        val plank = exercise(id = "ex-plank", name = "Plank", trackingType = ExerciseTrackingType.DURATION)
        val useCase = StampTemplateExerciseSnapshotsUseCase(FakeExerciseRepository(listOf(squat, plank)))

        val input = template(
            listOf(
                templateExercise(id = "te-1", exerciseId = "ex-squat", orderIndex = 0),
                templateExercise(id = "te-2", exerciseId = "ex-plank", orderIndex = 1)
            )
        )

        val result = useCase(input)

        assertEquals("Squat", result.templateExercises[0].exerciseName)
        assertEquals(ExerciseTrackingType.WEIGHT_REPS, result.templateExercises[0].exerciseTrackingType)
        assertEquals("Plank", result.templateExercises[1].exerciseName)
        assertEquals(ExerciseTrackingType.DURATION, result.templateExercises[1].exerciseTrackingType)
    }

    @Test
    fun `preserves every other field on the template exercise unchanged`() = runBlocking {
        val ex = exercise(id = "ex-1")
        val useCase = StampTemplateExerciseSnapshotsUseCase(FakeExerciseRepository(listOf(ex)))

        val original = templateExercise(exerciseId = "ex-1", plannedSets = 5).copy(
            plannedReps = 12,
            restSeconds = 90,
            notes = "controlled tempo",
            plannedWeightKg = 40f
        )
        val input = template(listOf(original))

        val result = useCase(input)

        val stamped = result.templateExercises.single()
        assertEquals(original.id, stamped.id)
        assertEquals(original.plannedSets, stamped.plannedSets)
        assertEquals(original.plannedReps, stamped.plannedReps)
        assertEquals(original.restSeconds, stamped.restSeconds)
        assertEquals(original.notes, stamped.notes)
        assertEquals(original.plannedWeightKg, stamped.plannedWeightKg)
    }

    @Test
    fun `throws when the referenced exercise cannot be found`() {
        val useCase = StampTemplateExerciseSnapshotsUseCase(FakeExerciseRepository(emptyList()))

        val input = template(listOf(templateExercise(exerciseId = "missing-exercise")))

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(input) }
        }
        assertEquals("Exercise missing-exercise not found", exception.message)
    }
}
