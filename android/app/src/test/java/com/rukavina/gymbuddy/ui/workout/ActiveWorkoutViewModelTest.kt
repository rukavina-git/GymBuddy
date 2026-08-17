package com.rukavina.gymbuddy.ui.workout

import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.TemplateExercise
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.usecase.exercise.GetAllExercisesIncludingHiddenUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.CreateWorkoutSessionUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.ValidateWorkoutSessionSetsUseCase
import com.rukavina.gymbuddy.testutil.FakeExerciseRepository
import com.rukavina.gymbuddy.testutil.FakeWorkoutSessionRepository
import com.rukavina.gymbuddy.testutil.FixedIdGenerator
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import com.rukavina.gymbuddy.testutil.testPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * ActiveWorkoutViewModel holds the active-workout state machine - starting
 * from a template, tracking sets per exercise, and the stop flow (save or
 * reject-and-stay). Every behavioural bug found during schema hardening
 * lived here, so these tests exercise that state machine directly against
 * fakes rather than mocks.
 *
 * Plain JUnit, deliberately not Robolectric: AppPreferencesRepository takes
 * a DataStore<Preferences> directly, not a Context, so a temp-file-backed
 * DataStore is enough here. That matters for more than convenience - JaCoCo's
 * runtime agent instruments classes as the JVM's normal classloader loads
 * them, but Robolectric reloads the entire app+test classpath through its
 * own sandbox classloader for shadowing, which the agent's transformer never
 * sees. A Robolectric-run test class executes correctly but silently
 * contributes zero coverage for every class it touches - confirmed by
 * inspecting the raw .exec file, which contained no trace of any ViewModel
 * or repository class after an earlier Robolectric-based version of this
 * suite ran green. Avoid Robolectric wherever a real Context isn't actually
 * required, or the coverage numbers will undercount real test coverage.
 */
class ActiveWorkoutViewModelTest {

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

    private val plank = Exercise(
        id = "ex-plank",
        name = "Plank",
        primaryMuscles = listOf(MuscleGroup.CORE),
        secondaryMuscles = emptyList(),
        difficulty = DifficultyLevel.BEGINNER,
        equipmentNeeded = emptyList(),
        category = ExerciseCategory.STRENGTH,
        exerciseType = ExerciseType.ISOLATION,
        trackingType = ExerciseTrackingType.DURATION
    )

    private val sprint = Exercise(
        id = "ex-sprint",
        name = "Sprint",
        primaryMuscles = listOf(MuscleGroup.LEGS),
        secondaryMuscles = emptyList(),
        difficulty = DifficultyLevel.INTERMEDIATE,
        equipmentNeeded = emptyList(),
        category = ExerciseCategory.CARDIO,
        exerciseType = ExerciseType.COMPOUND,
        trackingType = ExerciseTrackingType.WEIGHT_DISTANCE
    )

    private fun templateExercise(
        exerciseId: String,
        orderIndex: Int,
        plannedSets: Int = 3,
        plannedReps: Int? = 8,
        restSeconds: Int? = 90
    ) = TemplateExercise(
        id = "te-$exerciseId",
        exerciseId = exerciseId,
        exerciseName = "",
        exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
        plannedSets = plannedSets,
        plannedReps = plannedReps,
        orderIndex = orderIndex,
        restSeconds = restSeconds
    )

    private fun buildViewModel(
        exerciseRepo: FakeExerciseRepository = FakeExerciseRepository(listOf(benchPress, plank, sprint)),
        sessionRepo: FakeWorkoutSessionRepository = FakeWorkoutSessionRepository(),
        idGenerator: FixedIdGenerator = FixedIdGenerator((0..20).map { "id-$it" })
    ): Triple<ActiveWorkoutViewModel, FakeWorkoutSessionRepository, FixedIdGenerator> {
        val appPreferencesRepository = AppPreferencesRepository(testPreferencesDataStore())
        val viewModel = ActiveWorkoutViewModel(
            createWorkoutSessionUseCase = CreateWorkoutSessionUseCase(
                sessionRepo,
                idGenerator,
                ValidateWorkoutSessionSetsUseCase(exerciseRepo)
            ),
            getAllExercisesIncludingHiddenUseCase = GetAllExercisesIncludingHiddenUseCase(exerciseRepo),
            appPreferencesRepository = appPreferencesRepository,
            idGenerator = idGenerator
        )
        return Triple(viewModel, sessionRepo, idGenerator)
    }

    // --- starting a workout from a template ---

    @Test
    fun `starting a workout from a template populates exercises correctly`() {
        val (viewModel, _, _) = buildViewModel()
        val template = WorkoutTemplate(
            id = "template-1",
            title = "Push Day",
            templateExercises = listOf(
                templateExercise("ex-bench", orderIndex = 0, plannedSets = 3, plannedReps = 8, restSeconds = 90),
                templateExercise("ex-plank", orderIndex = 1, plannedSets = 1, plannedReps = null, restSeconds = null)
            )
        )

        viewModel.startWorkoutFromTemplate(template)
        val state = viewModel.uiState.value

        assertEquals("Push Day", state.workoutTitle)
        assertEquals("template-1", state.templateId)
        assertEquals("Push Day", state.templateTitle)
        assertTrue(state.isTimerRunning)
        assertFalse(state.workoutSaved)
        assertEquals(2, state.exercises.size)

        val benchExercise = state.exercises[0]
        assertEquals("ex-bench", benchExercise.exerciseId)
        assertEquals("Bench Press", benchExercise.exerciseName)
        assertEquals(ExerciseTrackingType.WEIGHT_REPS, benchExercise.exerciseTrackingType)
        assertEquals(3, benchExercise.sets.size)
        assertEquals(listOf(1, 2, 3), benchExercise.sets.map { it.setNumber })
        assertTrue(benchExercise.sets.all { it.reps.isEmpty() && it.weight.isEmpty() && !it.isCompleted })

        val plankExercise = state.exercises[1]
        assertEquals("ex-plank", plankExercise.exerciseId)
        assertEquals("Plank", plankExercise.exerciseName)
        assertEquals(ExerciseTrackingType.DURATION, plankExercise.exerciseTrackingType)
        assertEquals(1, plankExercise.sets.size)
    }

    @Test
    fun `starting a new workout resets any previous session state`() {
        val (viewModel, _, _) = buildViewModel()
        val firstTemplate = WorkoutTemplate(
            id = "template-1",
            title = "Push Day",
            templateExercises = listOf(templateExercise("ex-bench", orderIndex = 0))
        )
        val secondTemplate = WorkoutTemplate(
            id = "template-2",
            title = "Leg Day",
            templateExercises = listOf(templateExercise("ex-sprint", orderIndex = 0, plannedSets = 2))
        )

        viewModel.startWorkoutFromTemplate(firstTemplate)
        viewModel.updateSetReps(
            viewModel.uiState.value.exercises[0].id,
            viewModel.uiState.value.exercises[0].sets[0].id,
            "5"
        )
        viewModel.startWorkoutFromTemplate(secondTemplate)

        val state = viewModel.uiState.value
        assertEquals("Leg Day", state.workoutTitle)
        assertEquals(1, state.exercises.size)
        assertEquals("ex-sprint", state.exercises[0].exerciseId)
        assertEquals(0L, state.elapsedSeconds)
    }

    // --- the stop flow: empty workout ---

    @Test
    fun `saving an empty workout stays on screen without navigating or persisting`() {
        val (viewModel, sessionRepo, _) = buildViewModel()
        val template = WorkoutTemplate(
            id = "template-1",
            title = "Push Day",
            templateExercises = listOf(templateExercise("ex-bench", orderIndex = 0))
        )
        viewModel.startWorkoutFromTemplate(template)

        // No sets filled in - nothing completed.
        viewModel.saveWorkout()

        val state = viewModel.uiState.value
        assertFalse(state.workoutSaved)
        assertFalse(state.workoutDiscarded)
        assertFalse(state.isLoading)
        assertEquals("No exercises logged. Workout not saved.", state.errorMessage)
        assertTrue(sessionRepo.created.isEmpty())
    }

    @Test
    fun `rejected empty-workout stop leaves the timer running and does not clear exercises`() {
        val (viewModel, _, _) = buildViewModel()
        val template = WorkoutTemplate(
            id = "template-1",
            title = "Push Day",
            templateExercises = listOf(templateExercise("ex-bench", orderIndex = 0))
        )
        viewModel.startWorkoutFromTemplate(template)

        viewModel.saveWorkout()

        val state = viewModel.uiState.value
        assertTrue("timer must resume so the in-progress workout keeps ticking", state.isTimerRunning)
        assertEquals(1, state.exercises.size)
        assertTrue("hasActiveWorkout must still report true - this is a rejected stop, not a discard", viewModel.hasActiveWorkout())
    }

    // --- a failed save (persistence error) ---

    @Test
    fun `a failed save leaves exercises intact and the timer running`() {
        // startWorkoutFromTemplate resolves "ex-bench" from an exercise repo
        // that has it (so the workout starts normally), but the use case's
        // own validation repo is empty - CreateWorkoutSessionUseCase then
        // fails with "Exercise ... not found" at save time regardless of
        // what was typed into the set.
        val sessionRepo = FakeWorkoutSessionRepository()
        val idGenerator = FixedIdGenerator((0..20).map { "id-$it" })
        val appPreferencesRepository = AppPreferencesRepository(testPreferencesDataStore())
        val viewModel = ActiveWorkoutViewModel(
            createWorkoutSessionUseCase = CreateWorkoutSessionUseCase(
                sessionRepo,
                idGenerator,
                ValidateWorkoutSessionSetsUseCase(FakeExerciseRepository(emptyList()))
            ),
            getAllExercisesIncludingHiddenUseCase = GetAllExercisesIncludingHiddenUseCase(
                FakeExerciseRepository(listOf(benchPress))
            ),
            appPreferencesRepository = appPreferencesRepository,
            idGenerator = idGenerator
        )

        val template = WorkoutTemplate(
            id = "template-1",
            title = "Push Day",
            templateExercises = listOf(templateExercise("ex-bench", orderIndex = 0, plannedSets = 1))
        )
        viewModel.startWorkoutFromTemplate(template)
        val exercise = viewModel.uiState.value.exercises[0]
        viewModel.updateSetReps(exercise.id, exercise.sets[0].id, "10")
        viewModel.updateSetWeight(exercise.id, exercise.sets[0].id, "60")

        viewModel.saveWorkout()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.workoutSaved)
        assertTrue(state.errorMessage != null)
        assertTrue("timer must resume after a failed save", state.isTimerRunning)
        assertEquals(1, state.exercises.size)
        assertEquals("10", state.exercises[0].sets[0].reps)
        assertEquals("60", state.exercises[0].sets[0].weight)
        assertTrue(sessionRepo.created.isEmpty())
    }

    // --- a successful save ---

    @Test
    fun `a successful save clears state and marks the workout saved`() {
        val (viewModel, sessionRepo, _) = buildViewModel()
        val template = WorkoutTemplate(
            id = "template-1",
            title = "Push Day",
            templateExercises = listOf(templateExercise("ex-bench", orderIndex = 0, plannedSets = 1))
        )
        viewModel.startWorkoutFromTemplate(template)
        val exercise = viewModel.uiState.value.exercises[0]
        viewModel.updateSetReps(exercise.id, exercise.sets[0].id, "10")
        viewModel.updateSetWeight(exercise.id, exercise.sets[0].id, "60")

        viewModel.saveWorkout()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.workoutSaved)
        assertFalse(state.isTimerRunning)
        assertTrue(state.exercises.isEmpty())
        assertEquals(0L, state.elapsedSeconds)
        assertEquals("", state.workoutTitle)
        assertNull(state.errorMessage)

        val saved = sessionRepo.created.single()
        assertEquals("Push Day", saved.title)
        assertEquals(1, saved.performedExercises.size)
        assertEquals("Bench Press", saved.performedExercises[0].exerciseName)
    }

    @Test
    fun `an exercise with no completed sets is dropped but the workout still saves if another exercise qualifies`() {
        val (viewModel, sessionRepo, _) = buildViewModel()
        val template = WorkoutTemplate(
            id = "template-1",
            title = "Push Day",
            templateExercises = listOf(
                templateExercise("ex-bench", orderIndex = 0, plannedSets = 1),
                templateExercise("ex-plank", orderIndex = 1, plannedSets = 1, plannedReps = null)
            )
        )
        viewModel.startWorkoutFromTemplate(template)
        val benchExercise = viewModel.uiState.value.exercises.single { it.exerciseId == "ex-bench" }
        viewModel.updateSetReps(benchExercise.id, benchExercise.sets[0].id, "10")
        // Plank's only set is left untouched - not filled in, so it's dropped.

        viewModel.saveWorkout()

        val saved = sessionRepo.created.single()
        assertEquals(1, saved.performedExercises.size)
        assertEquals("ex-bench", saved.performedExercises[0].exerciseId)
        assertEquals(0, saved.performedExercises[0].orderIndex)
    }

    // --- set updates per tracking type write the right field ---

    @Test
    fun `updateSetReps and updateSetWeight write to a WEIGHT_REPS exercise's reps and weight fields`() {
        val (viewModel, _, _) = buildViewModel()
        val template = WorkoutTemplate(
            id = "template-1",
            title = "Push Day",
            templateExercises = listOf(templateExercise("ex-bench", orderIndex = 0, plannedSets = 1))
        )
        viewModel.startWorkoutFromTemplate(template)
        val exercise = viewModel.uiState.value.exercises[0]
        val setId = exercise.sets[0].id

        viewModel.updateSetReps(exercise.id, setId, "12")
        viewModel.updateSetWeight(exercise.id, setId, "82.5")

        val set = viewModel.uiState.value.exercises[0].sets[0]
        assertEquals("12", set.reps)
        assertEquals("82.5", set.weight)
        assertEquals("", set.duration)
        assertEquals("", set.distance)
    }

    @Test
    fun `updateSetDuration writes to a DURATION exercise's duration field`() {
        val (viewModel, _, _) = buildViewModel()
        val template = WorkoutTemplate(
            id = "template-1",
            title = "Core",
            templateExercises = listOf(templateExercise("ex-plank", orderIndex = 0, plannedSets = 1, plannedReps = null))
        )
        viewModel.startWorkoutFromTemplate(template)
        val exercise = viewModel.uiState.value.exercises[0]
        val setId = exercise.sets[0].id

        viewModel.updateSetDuration(exercise.id, setId, "45")

        val set = viewModel.uiState.value.exercises[0].sets[0]
        assertEquals("45", set.duration)
        assertEquals("", set.reps)
        assertEquals("", set.weight)
    }

    @Test
    fun `numeric filters strip non-digit characters from reps and duration but allow a decimal point in weight and distance`() {
        val (viewModel, _, _) = buildViewModel()
        val template = WorkoutTemplate(
            id = "template-1",
            title = "Push Day",
            templateExercises = listOf(templateExercise("ex-bench", orderIndex = 0, plannedSets = 1))
        )
        viewModel.startWorkoutFromTemplate(template)
        val exercise = viewModel.uiState.value.exercises[0]
        val setId = exercise.sets[0].id

        viewModel.updateSetReps(exercise.id, setId, "1a2b")
        viewModel.updateSetWeight(exercise.id, setId, "8x2.5y")
        viewModel.updateSetDuration(exercise.id, setId, "4a5")
        viewModel.updateSetDistance(exercise.id, setId, "1x0.5y")

        val set = viewModel.uiState.value.exercises[0].sets[0]
        assertEquals("12", set.reps)
        assertEquals("82.5", set.weight)
        assertEquals("45", set.duration)
        assertEquals("10.5", set.distance)
    }

    @Test
    fun `saveWorkout persists only the measurement each tracking type shows`() {
        val (viewModel, sessionRepo, _) = buildViewModel()
        val template = WorkoutTemplate(
            id = "template-1",
            title = "Mixed",
            templateExercises = listOf(
                templateExercise("ex-bench", orderIndex = 0, plannedSets = 1),
                templateExercise("ex-plank", orderIndex = 1, plannedSets = 1, plannedReps = null),
                templateExercise("ex-sprint", orderIndex = 2, plannedSets = 1, plannedReps = null)
            )
        )
        viewModel.startWorkoutFromTemplate(template)

        val benchExercise = viewModel.uiState.value.exercises.single { it.exerciseId == "ex-bench" }
        viewModel.updateSetReps(benchExercise.id, benchExercise.sets[0].id, "10")
        viewModel.updateSetWeight(benchExercise.id, benchExercise.sets[0].id, "60")

        val plankExercise = viewModel.uiState.value.exercises.single { it.exerciseId == "ex-plank" }
        viewModel.updateSetDuration(plankExercise.id, plankExercise.sets[0].id, "60")

        val sprintExercise = viewModel.uiState.value.exercises.single { it.exerciseId == "ex-sprint" }
        viewModel.updateSetWeight(sprintExercise.id, sprintExercise.sets[0].id, "0")
        viewModel.updateSetDistance(sprintExercise.id, sprintExercise.sets[0].id, "400")

        viewModel.saveWorkout()

        val saved = sessionRepo.created.single()
        val benchSet = saved.performedExercises.single { it.exerciseId == "ex-bench" }.sets.single()
        assertEquals(10, benchSet.reps)
        assertEquals(60f, benchSet.weightKg)
        assertNull(benchSet.durationSeconds)
        assertNull(benchSet.distanceMeters)

        val plankSet = saved.performedExercises.single { it.exerciseId == "ex-plank" }.sets.single()
        assertNull(plankSet.reps)
        assertNull(plankSet.weightKg)
        assertEquals(60, plankSet.durationSeconds)
        assertNull(plankSet.distanceMeters)

        val sprintSet = saved.performedExercises.single { it.exerciseId == "ex-sprint" }.sets.single()
        assertNull(sprintSet.reps)
        assertEquals(0f, sprintSet.weightKg)
        assertNull(sprintSet.durationSeconds)
        assertEquals(400f, sprintSet.distanceMeters)
    }

    @Test
    fun `saveWorkout converts weight from the preferred display unit to metric kg for storage`() {
        val (_, sessionRepo, _) = buildViewModel()
        val appPreferencesRepository = AppPreferencesRepository(testPreferencesDataStore())
        // setPreferredUnits persists to DataStore and the ViewModel's
        // loadUserPreferences() collector (started in init) picks it up -
        // both go through the same DataStore instance, since it's passed
        // in directly rather than re-derived from a Context.
        runTest {
            appPreferencesRepository.setPreferredUnits(PreferredUnits.IMPERIAL)
        }

        val vmWithImperialPrefs = ActiveWorkoutViewModel(
            createWorkoutSessionUseCase = CreateWorkoutSessionUseCase(
                sessionRepo,
                FixedIdGenerator((0..20).map { "id-$it" }),
                ValidateWorkoutSessionSetsUseCase(FakeExerciseRepository(listOf(benchPress)))
            ),
            getAllExercisesIncludingHiddenUseCase = GetAllExercisesIncludingHiddenUseCase(
                FakeExerciseRepository(listOf(benchPress))
            ),
            appPreferencesRepository = appPreferencesRepository,
            idGenerator = FixedIdGenerator((0..20).map { "id-$it" })
        )

        // DataStore's read is genuine background-thread I/O, not virtual
        // time - UnconfinedTestDispatcher makes loadUserPreferences()'s
        // collector start eagerly, but its first emission can still arrive
        // after this point. runBlocking here really waits (wall-clock, not
        // test-scheduler) for that first real emission instead of racing it.
        kotlinx.coroutines.runBlocking {
            vmWithImperialPrefs.uiState.first { it.preferredUnits == PreferredUnits.IMPERIAL }
        }

        val template = WorkoutTemplate(
            id = "template-1",
            title = "Push Day",
            templateExercises = listOf(templateExercise("ex-bench", orderIndex = 0, plannedSets = 1))
        )
        vmWithImperialPrefs.startWorkoutFromTemplate(template)
        assertEquals(PreferredUnits.IMPERIAL, vmWithImperialPrefs.uiState.value.preferredUnits)

        val exercise = vmWithImperialPrefs.uiState.value.exercises[0]
        vmWithImperialPrefs.updateSetReps(exercise.id, exercise.sets[0].id, "10")
        // 220 lbs entered - must be stored as kg, not the raw display value.
        vmWithImperialPrefs.updateSetWeight(exercise.id, exercise.sets[0].id, "220")

        vmWithImperialPrefs.saveWorkout()

        val savedSet = sessionRepo.created.single().performedExercises.single().sets.single()
        assertEquals(99.79f, savedSet.weightKg!!, 0.01f)
    }
}
