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
import com.rukavina.gymbuddy.testutil.FakeWorkoutTemplateRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateWorkoutTemplateUseCaseTest {

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

    private fun templateExercise(
        id: String = "te-1",
        exerciseId: String = "ex-1",
        plannedSets: Int = 3,
        plannedReps: Int? = 10,
        orderIndex: Int = 0,
        restSeconds: Int? = 90
    ) = TemplateExercise(
        id = id,
        exerciseId = exerciseId,
        exerciseName = "",
        exerciseTrackingType = ExerciseTrackingType.REPS_ONLY,
        plannedSets = plannedSets,
        plannedReps = plannedReps,
        orderIndex = orderIndex,
        restSeconds = restSeconds
    )

    private fun template(
        id: String = "template-1",
        title: String = "Push Day",
        templateExercises: List<TemplateExercise> = listOf(templateExercise())
    ) = WorkoutTemplate(id = id, title = title, templateExercises = templateExercises)

    private fun buildUseCase(
        exerciseRepo: FakeExerciseRepository = FakeExerciseRepository(listOf(exercise)),
        templateRepo: FakeWorkoutTemplateRepository = FakeWorkoutTemplateRepository()
    ) = UpdateWorkoutTemplateUseCase(templateRepo, StampTemplateExerciseSnapshotsUseCase(exerciseRepo)) to templateRepo

    @Test
    fun `persists the snapshot-stamped template on success`() = runBlocking {
        val (useCase, templateRepo) = buildUseCase()

        val result = useCase(template())

        assertTrue(result.isSuccess)
        assertEquals("Bench Press", templateRepo.updated.single().templateExercises.single().exerciseName)
    }

    @Test
    fun `fails without persisting when template id is blank`() = runBlocking {
        val (useCase, templateRepo) = buildUseCase()

        val result = useCase(template(id = ""))

        assertTrue(result.isFailure)
        assertEquals("Template ID cannot be blank", result.exceptionOrNull()?.message)
        assertTrue(templateRepo.updated.isEmpty())
    }

    @Test
    fun `fails without persisting when title is blank`() = runBlocking {
        val (useCase, templateRepo) = buildUseCase()

        val result = useCase(template(title = ""))

        assertTrue(result.isFailure)
        assertEquals("Template title cannot be blank", result.exceptionOrNull()?.message)
        assertTrue(templateRepo.updated.isEmpty())
    }

    @Test
    fun `fails without persisting when there are no exercises`() = runBlocking {
        val (useCase, templateRepo) = buildUseCase()

        val result = useCase(template(templateExercises = emptyList()))

        assertTrue(result.isFailure)
        assertEquals("Template must have at least one exercise", result.exceptionOrNull()?.message)
        assertTrue(templateRepo.updated.isEmpty())
    }

    @Test
    fun `fails without persisting when a template exercise id is blank - unlike create, update requires it upfront`() = runBlocking {
        val (useCase, templateRepo) = buildUseCase()

        val result = useCase(template(templateExercises = listOf(templateExercise(id = ""))))

        assertTrue(result.isFailure)
        assertEquals("Exercise ID must be valid", result.exceptionOrNull()?.message)
        assertTrue(templateRepo.updated.isEmpty())
    }

    @Test
    fun `fails without persisting when plannedSets is zero`() = runBlocking {
        val (useCase, templateRepo) = buildUseCase()

        val result = useCase(template(templateExercises = listOf(templateExercise(plannedSets = 0))))

        assertTrue(result.isFailure)
        assertEquals("Planned sets must be greater than 0", result.exceptionOrNull()?.message)
        assertTrue(templateRepo.updated.isEmpty())
    }

    @Test
    fun `fails without persisting when orderIndex is negative`() = runBlocking {
        val (useCase, templateRepo) = buildUseCase()

        val result = useCase(template(templateExercises = listOf(templateExercise(orderIndex = -1))))

        assertTrue(result.isFailure)
        assertEquals("Order index must be non-negative", result.exceptionOrNull()?.message)
        assertTrue(templateRepo.updated.isEmpty())
    }

    @Test
    fun `fails without persisting when a referenced exercise cannot be found`() = runBlocking {
        val (useCase, templateRepo) = buildUseCase(exerciseRepo = FakeExerciseRepository(emptyList()))

        val result = useCase(template())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(templateRepo.updated.isEmpty())
    }
}
