package com.rukavina.gymbuddy.ui.workout

import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.model.TemplateExercise
import com.rukavina.gymbuddy.domain.usecase.workout.CreateWorkoutSessionUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.DeleteWorkoutSessionUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.GetAllWorkoutSessionsUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.GetWorkoutSessionByIdUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.GetWorkoutSessionsByDateRangeUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.UpdateWorkoutSessionUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.ValidateWorkoutSessionSetsUseCase
import com.rukavina.gymbuddy.testutil.FakeExerciseRepository
import com.rukavina.gymbuddy.testutil.FakeWorkoutSessionRepository
import com.rukavina.gymbuddy.testutil.FixedIdGenerator
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import com.rukavina.gymbuddy.testutil.testPreferencesDataStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Plain JUnit, deliberately not Robolectric - see ActiveWorkoutViewModelTest's
// file comment for why: AppPreferencesRepository takes a DataStore directly,
// and Robolectric's classloader is invisible to JaCoCo's coverage agent.
class WorkoutSessionViewModelTest {

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
        sessionRepo: FakeWorkoutSessionRepository = FakeWorkoutSessionRepository(),
        idGenerator: FixedIdGenerator = FixedIdGenerator((0..20).map { "id-$it" })
    ): WorkoutSessionViewModel {
        val validate = ValidateWorkoutSessionSetsUseCase(exerciseRepo)
        return WorkoutSessionViewModel(
            getAllWorkoutSessionsUseCase = GetAllWorkoutSessionsUseCase(sessionRepo),
            getWorkoutSessionByIdUseCase = GetWorkoutSessionByIdUseCase(sessionRepo),
            getWorkoutSessionsByDateRangeUseCase = GetWorkoutSessionsByDateRangeUseCase(sessionRepo),
            createWorkoutSessionUseCase = CreateWorkoutSessionUseCase(sessionRepo, idGenerator, validate),
            updateWorkoutSessionUseCase = UpdateWorkoutSessionUseCase(sessionRepo, validate),
            deleteWorkoutSessionUseCase = DeleteWorkoutSessionUseCase(sessionRepo),
            appPreferencesRepository = AppPreferencesRepository(testPreferencesDataStore()),
            idGenerator = idGenerator
        )
    }

    private fun draft(
        title: String = "Push Day",
        performedExercises: List<PerformedExerciseDraft> = listOf(
            PerformedExerciseDraft(
                existingId = null,
                exerciseId = "ex-bench",
                exerciseName = "Bench Press",
                exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
                orderIndex = 0,
                sets = listOf(
                    WorkoutSetDraft(existingId = null, weightKg = 60f, reps = 8, durationSeconds = null, distanceMeters = null, orderIndex = 0)
                )
            )
        )
    ) = WorkoutSessionDraft(
        startedAt = 1_000L,
        endedAt = 2_000L,
        durationSeconds = 1000,
        title = title,
        notes = "felt good",
        templateId = null,
        templateTitle = null,
        performedExercises = performedExercises
    )

    // --- create (save) path ---

    @Test
    fun `createWorkoutSession mints ids for every new exercise and set`() {
        val viewModel = buildViewModel()

        viewModel.createWorkoutSession(draft())

        val state = viewModel.uiState.value
        assertEquals("Workout session created successfully", state.successMessage)
        assertNull(state.errorMessage)
    }

    @Test
    fun `createWorkoutSession persists a session whose sets and exercises carry minted ids`() {
        val sessionRepo = FakeWorkoutSessionRepository()
        val viewModel = buildViewModel(sessionRepo = sessionRepo)

        viewModel.createWorkoutSession(draft())

        val saved = sessionRepo.created.single()
        val performedExercise = saved.performedExercises.single()
        assertTrue(performedExercise.id.isNotBlank())
        assertTrue(performedExercise.sets.single().id.isNotBlank())
        assertEquals("Bench Press", performedExercise.exerciseName)
        assertEquals(ExerciseCategory.STRENGTH, performedExercise.exerciseCategory)
    }

    @Test
    fun `createWorkoutSession reports an error and does not persist when a set is invalid`() {
        val sessionRepo = FakeWorkoutSessionRepository()
        val viewModel = buildViewModel(sessionRepo = sessionRepo)
        val invalidDraft = draft(
            performedExercises = listOf(
                PerformedExerciseDraft(
                    existingId = null,
                    exerciseId = "ex-bench",
                    exerciseName = "Bench Press",
                    exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
                    orderIndex = 0,
                    sets = listOf(
                        // No reps for a WEIGHT_REPS exercise.
                        WorkoutSetDraft(existingId = null, weightKg = 60f, reps = null, durationSeconds = null, distanceMeters = null, orderIndex = 0)
                    )
                )
            )
        )

        viewModel.createWorkoutSession(invalidDraft)

        val state = viewModel.uiState.value
        assertEquals("reps is required", state.errorMessage)
        assertNull(state.successMessage)
        assertTrue(sessionRepo.created.isEmpty())
    }

    // --- update (edit) path ---

    @Test
    fun `updateWorkoutSession preserves the session's own id and existing exercise and set ids`() {
        val sessionRepo = FakeWorkoutSessionRepository()
        val viewModel = buildViewModel(sessionRepo = sessionRepo)
        // updateWorkoutSession only reads existing.id from this - it does
        // not need to be a session actually stored in the repository.
        val existing = com.rukavina.gymbuddy.domain.model.WorkoutSession(
            id = "existing-session",
            startedAt = 500L,
            durationSeconds = 500,
            title = "Old title",
            performedExercises = emptyList()
        )
        val editDraft = draft(
            title = "Push Day (edited)",
            performedExercises = listOf(
                PerformedExerciseDraft(
                    existingId = "existing-pe",
                    exerciseId = "ex-bench",
                    exerciseName = "Bench Press",
                    exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
                    orderIndex = 0,
                    sets = listOf(
                        WorkoutSetDraft(existingId = "existing-set", weightKg = 70f, reps = 6, durationSeconds = null, distanceMeters = null, orderIndex = 0),
                        // A set added during this edit - no existingId.
                        WorkoutSetDraft(existingId = null, weightKg = 70f, reps = 5, durationSeconds = null, distanceMeters = null, orderIndex = 1)
                    )
                )
            )
        )

        viewModel.updateWorkoutSession(existing, editDraft)

        val updated = sessionRepo.updated.single()
        assertEquals("existing-session", updated.id)
        assertEquals("Push Day (edited)", updated.title)
        val performedExercise = updated.performedExercises.single()
        assertEquals("existing-pe", performedExercise.id)
        assertEquals("existing-set", performedExercise.sets[0].id)
        assertTrue("the added set must get a freshly minted id, not blank", performedExercise.sets[1].id.isNotBlank())
        assertTrue(performedExercise.sets[1].id != "existing-set")
    }

    @Test
    fun `updateWorkoutSession reports an error and does not persist when the session id is blank`() {
        val sessionRepo = FakeWorkoutSessionRepository()
        val viewModel = buildViewModel(sessionRepo = sessionRepo)
        val blankIdSession = com.rukavina.gymbuddy.domain.model.WorkoutSession(
            id = "",
            startedAt = 500L,
            durationSeconds = 500,
            title = "Old title",
            performedExercises = emptyList()
        )

        viewModel.updateWorkoutSession(blankIdSession, draft())

        val state = viewModel.uiState.value
        assertEquals("Workout session ID cannot be blank", state.errorMessage)
        assertTrue(sessionRepo.updated.isEmpty())
    }

    // --- starting a session directly from a template ---

    @Test
    fun `startSessionFromTemplate persists a session pre-populated from the template`() {
        val sessionRepo = FakeWorkoutSessionRepository()
        val viewModel = buildViewModel(sessionRepo = sessionRepo)
        val template = WorkoutTemplate(
            id = "template-1",
            title = "Push Day",
            templateExercises = listOf(
                TemplateExercise(
                    id = "te-1",
                    exerciseId = "ex-bench",
                    exerciseName = "",
                    exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
                    plannedSets = 2,
                    plannedReps = 8,
                    orderIndex = 0
                )
            )
        )

        viewModel.startSessionFromTemplate(template)

        val state = viewModel.uiState.value
        assertEquals("Workout session started from template", state.successMessage)
        val saved = sessionRepo.created.single()
        assertEquals("template-1", saved.templateId)
        assertEquals("Push Day", saved.templateTitle)
        val performedExercise = saved.performedExercises.single()
        assertEquals(2, performedExercise.sets.size)
        assertEquals(8, performedExercise.sets[0].reps)
    }
}
