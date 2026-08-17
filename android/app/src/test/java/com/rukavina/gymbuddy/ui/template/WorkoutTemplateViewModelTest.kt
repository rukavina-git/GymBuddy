package com.rukavina.gymbuddy.ui.template

import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.usecase.exercise.GetAllExercisesIncludingHiddenUseCase
import com.rukavina.gymbuddy.domain.usecase.exercise.GetAllExercisesUseCase
import com.rukavina.gymbuddy.domain.usecase.template.CreateWorkoutTemplateUseCase
import com.rukavina.gymbuddy.domain.usecase.template.DeleteWorkoutTemplateUseCase
import com.rukavina.gymbuddy.domain.usecase.template.GetAllWorkoutTemplatesUseCase
import com.rukavina.gymbuddy.domain.usecase.template.GetWorkoutTemplateByIdUseCase
import com.rukavina.gymbuddy.domain.usecase.template.SearchWorkoutTemplatesUseCase
import com.rukavina.gymbuddy.domain.usecase.template.StampTemplateExerciseSnapshotsUseCase
import com.rukavina.gymbuddy.domain.usecase.template.UpdateWorkoutTemplateUseCase
import com.rukavina.gymbuddy.testutil.FakeExerciseRepository
import com.rukavina.gymbuddy.testutil.FakeWorkoutTemplateRepository
import com.rukavina.gymbuddy.testutil.FixedIdGenerator
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Plain JUnit, deliberately not Robolectric: WorkoutTemplateViewModel has no
// Context-dependent collaborator (unlike the other ViewModels here, it
// doesn't touch AppPreferencesRepository), and JaCoCo's runtime agent can't
// see coverage for classes loaded through Robolectric's own classloader -
// see ActiveWorkoutViewModelTest's file comment for the full story. Running
// this one for real lets its coverage actually count.
class WorkoutTemplateViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val benchPress = Exercise(
        id = "ex-bench",
        name = "Bench Press",
        primaryMuscles = listOf(MuscleGroup.CHEST),
        secondaryMuscles = emptyList(),
        difficulty = DifficultyLevel.INTERMEDIATE,
        equipmentNeeded = emptyList(),
        category = ExerciseCategory.STRENGTH,
        exerciseType = ExerciseType.COMPOUND,
        trackingType = ExerciseTrackingType.WEIGHT_REPS
    )

    private fun buildViewModel(
        exerciseRepo: FakeExerciseRepository = FakeExerciseRepository(listOf(benchPress)),
        templateRepo: FakeWorkoutTemplateRepository = FakeWorkoutTemplateRepository(),
        idGenerator: FixedIdGenerator = FixedIdGenerator((0..20).map { "id-$it" })
    ): WorkoutTemplateViewModel {
        val stamp = StampTemplateExerciseSnapshotsUseCase(exerciseRepo)
        return WorkoutTemplateViewModel(
            getAllWorkoutTemplatesUseCase = GetAllWorkoutTemplatesUseCase(templateRepo),
            getWorkoutTemplateByIdUseCase = GetWorkoutTemplateByIdUseCase(templateRepo),
            searchWorkoutTemplatesUseCase = SearchWorkoutTemplatesUseCase(templateRepo),
            createWorkoutTemplateUseCase = CreateWorkoutTemplateUseCase(templateRepo, idGenerator, stamp),
            updateWorkoutTemplateUseCase = UpdateWorkoutTemplateUseCase(templateRepo, stamp),
            deleteWorkoutTemplateUseCase = DeleteWorkoutTemplateUseCase(templateRepo),
            getAllExercisesUseCase = GetAllExercisesUseCase(exerciseRepo),
            getAllExercisesIncludingHiddenUseCase = GetAllExercisesIncludingHiddenUseCase(exerciseRepo),
            workoutTemplateRepository = templateRepo,
            idGenerator = idGenerator
        )
    }

    private fun exerciseDraft(
        localKey: Int = 0,
        existingId: String? = null,
        plannedSets: Int = 3,
        plannedReps: Int? = 8,
        orderIndex: Int = 0
    ) = TemplateExerciseDraft(
        localKey = localKey,
        existingId = existingId,
        exerciseId = "ex-bench",
        exerciseName = "Bench Press",
        exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
        plannedSets = plannedSets,
        plannedReps = plannedReps,
        orderIndex = orderIndex,
        restSeconds = 90,
        notes = null
    )

    // --- create (save) path ---

    @Test
    fun `createTemplate persists a template with a minted id and stamped exercise snapshot`() {
        val templateRepo = FakeWorkoutTemplateRepository()
        val viewModel = buildViewModel(templateRepo = templateRepo)

        viewModel.createTemplate(WorkoutTemplateDraft(title = "Push Day", exercises = listOf(exerciseDraft())))

        val state = viewModel.uiState.value
        assertEquals("Workout template created successfully", state.successMessage)
        val saved = templateRepo.created.single()
        assertTrue(saved.id.isNotBlank())
        assertEquals(EntitySource.CUSTOM, saved.source)
        assertEquals("Bench Press", saved.templateExercises.single().exerciseName)
    }

    @Test
    fun `createTemplate reports an error and does not persist when no exercises are given`() {
        val templateRepo = FakeWorkoutTemplateRepository()
        val viewModel = buildViewModel(templateRepo = templateRepo)

        viewModel.createTemplate(WorkoutTemplateDraft(title = "Empty", exercises = emptyList()))

        val state = viewModel.uiState.value
        assertEquals("Template must have at least one exercise", state.errorMessage)
        assertTrue(templateRepo.created.isEmpty())
    }

    // --- update (edit) path ---

    @Test
    fun `updateTemplate preserves the template's own id, source, owner and derivedFromId`() {
        val templateRepo = FakeWorkoutTemplateRepository()
        val viewModel = buildViewModel(templateRepo = templateRepo)
        val existing = WorkoutTemplate(
            id = "template-1",
            title = "Old title",
            templateExercises = emptyList(),
            source = EntitySource.DEFAULT,
            ownerId = null,
            derivedFromId = "derived-from-1"
        )

        viewModel.updateTemplate(existing, WorkoutTemplateDraft(title = "New title", exercises = listOf(exerciseDraft())))

        val updated = templateRepo.updated.single()
        assertEquals("template-1", updated.id)
        assertEquals("New title", updated.title)
        assertEquals(EntitySource.DEFAULT, updated.source)
        assertEquals("derived-from-1", updated.derivedFromId)
    }

    @Test
    fun `updateTemplate preserves existing exercise ids and mints ids only for new exercises`() {
        val templateRepo = FakeWorkoutTemplateRepository()
        val viewModel = buildViewModel(templateRepo = templateRepo)
        val existing = WorkoutTemplate(id = "template-1", title = "Push Day", templateExercises = emptyList())
        val editDraft = WorkoutTemplateDraft(
            title = "Push Day",
            exercises = listOf(
                exerciseDraft(localKey = 0, existingId = "existing-te", orderIndex = 0),
                exerciseDraft(localKey = 1, existingId = null, orderIndex = 1)
            )
        )

        viewModel.updateTemplate(existing, editDraft)

        val updated = templateRepo.updated.single()
        assertEquals("existing-te", updated.templateExercises[0].id)
        assertTrue(updated.templateExercises[1].id.isNotBlank())
        assertTrue(updated.templateExercises[1].id != "existing-te")
    }

    @Test
    fun `updateTemplate reports an error and does not persist when a referenced exercise cannot be found`() {
        val templateRepo = FakeWorkoutTemplateRepository()
        val viewModel = buildViewModel(exerciseRepo = FakeExerciseRepository(emptyList()), templateRepo = templateRepo)
        val existing = WorkoutTemplate(id = "template-1", title = "Push Day", templateExercises = emptyList())

        viewModel.updateTemplate(existing, WorkoutTemplateDraft(title = "Push Day", exercises = listOf(exerciseDraft())))

        val state = viewModel.uiState.value
        assertTrue(state.errorMessage != null)
        assertTrue(templateRepo.updated.isEmpty())
    }

    // --- hide/unhide ---

    @Test
    fun `hideTemplate marks the template hidden and unhideTemplate reverses it`() {
        val templateRepo = FakeWorkoutTemplateRepository()
        val viewModel = buildViewModel(templateRepo = templateRepo)

        viewModel.hideTemplate("template-1")
        assertEquals("Template hidden", viewModel.uiState.value.successMessage)

        viewModel.unhideTemplate("template-1")
        assertEquals("Template restored", viewModel.uiState.value.successMessage)
    }
}
