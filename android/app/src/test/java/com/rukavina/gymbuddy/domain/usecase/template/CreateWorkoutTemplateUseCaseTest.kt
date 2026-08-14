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
import com.rukavina.gymbuddy.testutil.FixedIdGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateWorkoutTemplateUseCaseTest {

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
        id: String = "",
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
        id: String = "",
        title: String = "Push Day",
        templateExercises: List<TemplateExercise> = listOf(templateExercise())
    ) = WorkoutTemplate(id = id, title = title, templateExercises = templateExercises)

    private fun buildUseCase(
        idGenerator: FixedIdGenerator = FixedIdGenerator("template-id", "te-id"),
        exerciseRepo: FakeExerciseRepository = FakeExerciseRepository(listOf(exercise)),
        templateRepo: FakeWorkoutTemplateRepository = FakeWorkoutTemplateRepository()
    ) = Triple(
        CreateWorkoutTemplateUseCase(templateRepo, idGenerator, StampTemplateExerciseSnapshotsUseCase(exerciseRepo)),
        templateRepo,
        idGenerator
    )

    @Test
    fun `generates ids for the template and every exercise when blank`() = runBlocking {
        val (useCase, templateRepo, _) = buildUseCase()

        val result = useCase(template())

        assertTrue(result.isSuccess)
        val saved = templateRepo.created.single()
        assertEquals("template-id", saved.id)
        assertEquals("te-id", saved.templateExercises.single().id)
    }

    @Test
    fun `preserves ids that are already set`() = runBlocking {
        val (useCase, templateRepo, idGenerator) = buildUseCase()

        val result = useCase(template(id = "existing-template", templateExercises = listOf(templateExercise(id = "existing-te"))))

        assertTrue(result.isSuccess)
        val saved = templateRepo.created.single()
        assertEquals("existing-template", saved.id)
        assertEquals("existing-te", saved.templateExercises.single().id)
        assertEquals(0, idGenerator.callCount)
    }

    @Test
    fun `persists the snapshot-stamped template`() = runBlocking {
        val (useCase, templateRepo, _) = buildUseCase()

        useCase(template())

        assertEquals("Bench Press", templateRepo.created.single().templateExercises.single().exerciseName)
    }

    @Test
    fun `fails without persisting when title is blank`() = runBlocking {
        val (useCase, templateRepo, _) = buildUseCase()

        val result = useCase(template(title = "  "))

        assertTrue(result.isFailure)
        assertEquals("Template title cannot be blank", result.exceptionOrNull()?.message)
        assertTrue(templateRepo.created.isEmpty())
    }

    @Test
    fun `fails without persisting when there are no exercises`() = runBlocking {
        val (useCase, templateRepo, _) = buildUseCase()

        val result = useCase(template(templateExercises = emptyList()))

        assertTrue(result.isFailure)
        assertEquals("Template must have at least one exercise", result.exceptionOrNull()?.message)
        assertTrue(templateRepo.created.isEmpty())
    }

    @Test
    fun `fails without persisting when plannedSets is zero`() = runBlocking {
        val (useCase, templateRepo, _) = buildUseCase()

        val result = useCase(template(templateExercises = listOf(templateExercise(plannedSets = 0))))

        assertTrue(result.isFailure)
        assertEquals("Planned sets must be greater than 0", result.exceptionOrNull()?.message)
        assertTrue(templateRepo.created.isEmpty())
    }

    @Test
    fun `fails without persisting when plannedReps is zero`() = runBlocking {
        val (useCase, templateRepo, _) = buildUseCase()

        val result = useCase(template(templateExercises = listOf(templateExercise(plannedReps = 0))))

        assertTrue(result.isFailure)
        assertEquals("Planned reps must be greater than 0 if specified", result.exceptionOrNull()?.message)
        assertTrue(templateRepo.created.isEmpty())
    }

    @Test
    fun `allows a null plannedReps - it is only validated when specified`() = runBlocking {
        val (useCase, templateRepo, _) = buildUseCase()

        val result = useCase(template(templateExercises = listOf(templateExercise(plannedReps = null))))

        assertTrue(result.isSuccess)
        assertEquals(1, templateRepo.created.size)
    }

    @Test
    fun `fails without persisting when orderIndex is negative`() = runBlocking {
        val (useCase, templateRepo, _) = buildUseCase()

        val result = useCase(template(templateExercises = listOf(templateExercise(orderIndex = -1))))

        assertTrue(result.isFailure)
        assertEquals("Order index must be non-negative", result.exceptionOrNull()?.message)
        assertTrue(templateRepo.created.isEmpty())
    }

    @Test
    fun `fails without persisting when restSeconds is zero`() = runBlocking {
        val (useCase, templateRepo, _) = buildUseCase()

        val result = useCase(template(templateExercises = listOf(templateExercise(restSeconds = 0))))

        assertTrue(result.isFailure)
        assertEquals("Rest seconds must be greater than 0 if specified", result.exceptionOrNull()?.message)
        assertTrue(templateRepo.created.isEmpty())
    }

    @Test
    fun `fails without persisting when exerciseId is blank`() = runBlocking {
        val (useCase, templateRepo, _) = buildUseCase()

        val result = useCase(template(templateExercises = listOf(templateExercise(exerciseId = ""))))

        assertTrue(result.isFailure)
        assertEquals("Exercise ID must be valid", result.exceptionOrNull()?.message)
        assertTrue(templateRepo.created.isEmpty())
    }

    @Test
    fun `fails without persisting when a referenced exercise cannot be found`() = runBlocking {
        val (useCase, templateRepo, _) = buildUseCase(exerciseRepo = FakeExerciseRepository(emptyList()))

        val result = useCase(template())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(templateRepo.created.isEmpty())
    }
}
