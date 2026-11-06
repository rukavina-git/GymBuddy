package com.rukavina.gymbuddy.ui.workout

import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.sync.InMemoryDataOwnerStore
import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.TemplateExercise
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.sync.RecordingSyncRequester
import com.rukavina.gymbuddy.domain.usecase.exercise.GetAllExercisesIncludingHiddenUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.CreateWorkoutSessionUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.ValidateWorkoutSessionSetsUseCase
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.repository.WorkoutSessionRepository
import com.rukavina.gymbuddy.testutil.FakeActiveWorkoutDraftRepository
import com.rukavina.gymbuddy.testutil.FakeRestBuzzScheduler
import com.rukavina.gymbuddy.testutil.FakeWorkoutServiceStarter
import com.rukavina.gymbuddy.testutil.FakeExerciseRepository
import com.rukavina.gymbuddy.testutil.FakeWorkoutSessionRepository
import com.rukavina.gymbuddy.testutil.FixedIdGenerator
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import com.rukavina.gymbuddy.testutil.MutableClock
import com.rukavina.gymbuddy.testutil.testPreferencesDataStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
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

    // DataStore reads hop to IO and resume on Main; cancel the scopes before
    // MainDispatcherRule resets Main, or they land in the next test class.
    private val created = mutableListOf<ActiveWorkoutViewModel>()

    @After
    fun cancelViewModels() = created.forEach { it.viewModelScope.cancel() }

    private fun buildViewModel(
        exerciseRepo: FakeExerciseRepository = FakeExerciseRepository(listOf(benchPress, plank, sprint)),
        sessionRepo: FakeWorkoutSessionRepository = FakeWorkoutSessionRepository(),
        idGenerator: FixedIdGenerator = FixedIdGenerator((0..20).map { "id-$it" }),
        sessionRepository: WorkoutSessionRepository = sessionRepo,
        draftRepo: FakeActiveWorkoutDraftRepository = FakeActiveWorkoutDraftRepository(),
        clock: MutableClock = MutableClock(),
        owners: InMemoryDataOwnerStore = InMemoryDataOwnerStore(),
        service: FakeWorkoutServiceStarter = FakeWorkoutServiceStarter(),
        buzz: FakeRestBuzzScheduler = FakeRestBuzzScheduler(),
    ): Triple<ActiveWorkoutViewModel, FakeWorkoutSessionRepository, FixedIdGenerator> {
        val appPreferencesRepository = AppPreferencesRepository(testPreferencesDataStore())
        val viewModel = ActiveWorkoutViewModel(
            createWorkoutSessionUseCase = CreateWorkoutSessionUseCase(
                sessionRepository,
                idGenerator,
                ValidateWorkoutSessionSetsUseCase(exerciseRepo),
                RecordingSyncRequester()
            ),
            getAllExercisesIncludingHiddenUseCase = GetAllExercisesIncludingHiddenUseCase(exerciseRepo),
            appPreferencesRepository = appPreferencesRepository,
            idGenerator = idGenerator,
            draftRepository = draftRepo,
            sessionRepository = sessionRepository,
            owners = owners,
            clock = clock,
            workoutService = service,
            restBuzz = buzz,
        )
        created += viewModel
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
        // Sets start from the template's targets (reps 8, no weight).
        assertTrue(benchExercise.sets.all { it.reps == "8" && it.weight.isEmpty() && !it.isCompleted })

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
                ValidateWorkoutSessionSetsUseCase(FakeExerciseRepository(emptyList())),
                RecordingSyncRequester()
            ),
            getAllExercisesIncludingHiddenUseCase = GetAllExercisesIncludingHiddenUseCase(
                FakeExerciseRepository(listOf(benchPress))
            ),
            appPreferencesRepository = appPreferencesRepository,
            idGenerator = idGenerator,
            draftRepository = FakeActiveWorkoutDraftRepository(),
            sessionRepository = FakeWorkoutSessionRepository(),
            owners = InMemoryDataOwnerStore(),
            clock = MutableClock(),
            workoutService = FakeWorkoutServiceStarter(),
            restBuzz = FakeRestBuzzScheduler(),
        )
        created += viewModel

        val template = WorkoutTemplate(
            id = "template-1",
            title = "Push Day",
            templateExercises = listOf(templateExercise("ex-bench", orderIndex = 0, plannedSets = 1))
        )
        viewModel.startWorkoutFromTemplate(template)
        val exercise = viewModel.uiState.value.exercises[0]
        viewModel.updateSetReps(exercise.id, exercise.sets[0].id, "10")
        viewModel.updateSetWeight(exercise.id, exercise.sets[0].id, "60")

        viewModel.done()
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

        viewModel.done()
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

        viewModel.done()
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

        viewModel.done()
        viewModel.done()
        viewModel.done()
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
                ValidateWorkoutSessionSetsUseCase(FakeExerciseRepository(listOf(benchPress))),
                RecordingSyncRequester()
            ),
            getAllExercisesIncludingHiddenUseCase = GetAllExercisesIncludingHiddenUseCase(
                FakeExerciseRepository(listOf(benchPress))
            ),
            appPreferencesRepository = appPreferencesRepository,
            idGenerator = FixedIdGenerator((0..20).map { "id-$it" }),
            draftRepository = FakeActiveWorkoutDraftRepository(),
            sessionRepository = FakeWorkoutSessionRepository(),
            owners = InMemoryDataOwnerStore(),
            clock = MutableClock(),
            workoutService = FakeWorkoutServiceStarter(),
            restBuzz = FakeRestBuzzScheduler(),
        )
        created += vmWithImperialPrefs

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

        vmWithImperialPrefs.done()
        vmWithImperialPrefs.saveWorkout()

        val savedSet = sessionRepo.created.single().performedExercises.single().sets.single()
        assertEquals(99.79f, savedSet.weightKg!!, 0.01f)
    }

    // --- repository failures and repeated taps ---

    private val oneSetBench = WorkoutTemplate(
        id = "template-1",
        title = "Push Day",
        templateExercises = listOf(templateExercise("ex-bench", orderIndex = 0, plannedSets = 1))
    )

    private fun ActiveWorkoutViewModel.fillFirstSet(reps: String, weight: String) {
        val exercise = uiState.value.exercises[0]
        updateSetReps(exercise.id, exercise.sets[0].id, reps)
        updateSetWeight(exercise.id, exercise.sets[0].id, weight)
        done()
    }

    /** Fails every create until [failing] is false; can also hold a create open on [gate]. */
    private class ControllableSessionRepository : WorkoutSessionRepository {
        var failing = false
        var gate: CompletableDeferred<Unit>? = null
        val created = mutableListOf<WorkoutSession>()
        var createCalls = 0

        override fun getAllWorkoutSessions(): Flow<List<WorkoutSession>> = flowOf(created)
        override suspend fun getWorkoutSessionById(id: String): WorkoutSession? = null
        override fun getWorkoutSessionsByDateRange(startDate: Long, endDate: Long) = flowOf(created.toList())
        override suspend fun createWorkoutSession(workoutSession: WorkoutSession) {
            createCalls++
            gate?.await()
            if (failing) throw IllegalStateException("disk full")
            created.add(workoutSession)
        }
        override suspend fun updateWorkoutSession(workoutSession: WorkoutSession) = Unit
        override suspend fun deleteWorkoutSession(id: String) = Unit
    }

    @Test
    fun `a repository exception on save keeps the workout and shows its message`() {
        val repo = ControllableSessionRepository().apply { failing = true }
        val draftRepo = FakeActiveWorkoutDraftRepository()
        val (viewModel, _, _) = buildViewModel(sessionRepository = repo, draftRepo = draftRepo)
        viewModel.startWorkoutFromTemplate(oneSetBench)
        viewModel.fillFirstSet("10", "60")

        viewModel.saveWorkout()

        val state = viewModel.uiState.value
        assertEquals("disk full", state.errorMessage)
        assertFalse(state.isLoading)
        assertFalse(state.workoutSaved)
        assertTrue(state.isTimerRunning)
        assertEquals("10", state.exercises[0].sets[0].reps)
        assertNotNull("the draft must survive a failed save", draftRepo.draft)
    }

    @Test
    fun `a second save tap while the first is running is ignored`() {
        val repo = ControllableSessionRepository().apply { gate = CompletableDeferred() }
        val (viewModel, _, _) = buildViewModel(sessionRepository = repo)
        viewModel.startWorkoutFromTemplate(oneSetBench)
        viewModel.fillFirstSet("10", "60")

        viewModel.saveWorkout()
        assertTrue(viewModel.uiState.value.isLoading)
        viewModel.saveWorkout()
        repo.gate!!.complete(Unit)

        assertEquals(1, repo.createCalls)
        assertEquals(1, repo.created.size)
        assertTrue(viewModel.uiState.value.workoutSaved)
    }

    @Test
    fun `set edits after a failed save are what the retry saves`() {
        val repo = ControllableSessionRepository().apply { failing = true }
        val (viewModel, _, _) = buildViewModel(sessionRepository = repo)
        viewModel.startWorkoutFromTemplate(oneSetBench)
        viewModel.fillFirstSet("10", "60")
        viewModel.saveWorkout()
        viewModel.clearError()

        viewModel.fillFirstSet("12", "70")
        repo.failing = false
        viewModel.saveWorkout()

        val savedSet = repo.created.single().performedExercises.single().sets.single()
        assertEquals(12, savedSet.reps)
        assertEquals(70f, savedSet.weightKg)
        assertTrue(viewModel.uiState.value.workoutSaved)
    }

    // --- the draft: surviving process death ---

    @Test
    fun `every set edit is written to the draft`() {
        val draftRepo = FakeActiveWorkoutDraftRepository()
        val (viewModel, _, _) = buildViewModel(draftRepo = draftRepo)
        viewModel.startWorkoutFromTemplate(oneSetBench)
        assertNotNull(draftRepo.draft)

        viewModel.fillFirstSet("10", "60")
        val exercise = viewModel.uiState.value.exercises[0]
        viewModel.updateSetNotes(exercise.id, exercise.sets[0].id, "slow negatives")

        val set = draftRepo.draft!!.exercises.single().sets.single()
        assertEquals("10", set.reps)
        assertEquals("60", set.weight)
        assertEquals("slow negatives", set.notes)
    }

    @Test
    fun `a fresh ViewModel restores the draft, counting time away while the timer ran`() {
        val draftRepo = FakeActiveWorkoutDraftRepository()
        val clock = MutableClock()
        val (first, _, _) = buildViewModel(draftRepo = draftRepo, clock = clock)
        first.startWorkoutFromTemplate(oneSetBench)
        first.fillFirstSet("10", "60")
        val before = first.uiState.value

        // Process death: the ViewModel is gone, the draft stays. 40 min pass.
        clock.advanceSeconds(40 * 60)
        val (second, sessionRepo, _) = buildViewModel(draftRepo = draftRepo, clock = clock)

        val restored = second.uiState.value
        assertTrue(second.hasActiveWorkout())
        assertEquals("Push Day", restored.workoutTitle)
        assertEquals("template-1", restored.templateId)
        assertEquals(before.workoutStartTime, restored.workoutStartTime)
        assertEquals(before.exercises, restored.exercises)
        assertTrue(restored.isTimerRunning)
        assertEquals(before.elapsedSeconds + 40 * 60, restored.elapsedSeconds)

        second.saveWorkout()
        assertEquals("Push Day", sessionRepo.created.single().title)
        assertNull("a saved workout must not come back", draftRepo.draft)
    }

    @Test
    fun `a paused timer stays paused on restore and away time is not counted`() {
        val draftRepo = FakeActiveWorkoutDraftRepository()
        val clock = MutableClock()
        val (first, _, _) = buildViewModel(draftRepo = draftRepo, clock = clock)
        first.startWorkoutFromTemplate(oneSetBench)
        first.toggleTimer()
        val pausedAt = first.uiState.value.elapsedSeconds

        clock.advanceSeconds(600)
        val (second, _, _) = buildViewModel(draftRepo = draftRepo, clock = clock)

        assertFalse(second.uiState.value.isTimerRunning)
        assertEquals(pausedAt, second.uiState.value.elapsedSeconds)
    }

    @Test
    fun `discarding deletes the draft so a fresh ViewModel starts empty`() {
        val draftRepo = FakeActiveWorkoutDraftRepository()
        val (first, _, _) = buildViewModel(draftRepo = draftRepo)
        first.startWorkoutFromTemplate(oneSetBench)
        first.fillFirstSet("10", "60")

        first.discardWorkout()
        assertNull(draftRepo.draft)

        val (second, _, _) = buildViewModel(draftRepo = draftRepo)
        assertFalse(second.hasActiveWorkout())
    }

    @Test
    fun `a rejected empty save keeps the draft`() {
        val draftRepo = FakeActiveWorkoutDraftRepository()
        val (viewModel, _, _) = buildViewModel(draftRepo = draftRepo)
        viewModel.startWorkoutFromTemplate(oneSetBench)

        viewModel.saveWorkout()

        assertNotNull(draftRepo.draft)
    }

    // --- Plan 7 06a: logging ---

    private val twoExercises = WorkoutTemplate(
        id = "template-2",
        title = "Push A",
        templateExercises = listOf(
            templateExercise("ex-bench", orderIndex = 0, plannedSets = 2, plannedReps = 8, restSeconds = 90),
            templateExercise("ex-plank", orderIndex = 1, plannedSets = 1, plannedReps = null, restSeconds = null)
        )
    )

    /** Events sent so far (the channel is buffered). */
    private fun ActiveWorkoutViewModel.drainEvents(): List<WorkoutEvent> {
        val out = mutableListOf<WorkoutEvent>()
        kotlinx.coroutines.runBlocking {
            kotlinx.coroutines.withTimeoutOrNull(50) { events.collect { out += it } }
        }
        return out
    }

    @Test
    fun `done logs the current set, starts its rest and carries its values to the next set`() {
        val (viewModel, _, _) = buildViewModel()
        viewModel.startWorkoutFromTemplate(twoExercises)
        viewModel.step(SetField.Weight, plus = true) // 0 → 2.5
        viewModel.step(SetField.Reps, plus = true) // 8 → 9

        viewModel.done()

        val state = viewModel.uiState.value
        val bench = state.exercises[0]
        assertTrue(bench.sets[0].isCompleted)
        assertEquals("2.5", bench.sets[1].weight)
        assertEquals("9", bench.sets[1].reps)
        assertEquals(RestState(bench.sets[0].id, state.elapsedSeconds, 90), state.rest)
        assertEquals(0, state.currentExerciseIndex)
        assertEquals(listOf(WorkoutEvent.SetDone), viewModel.drainEvents())
    }

    @Test
    fun `finishing an exercise moves to the next one with sets left, and the last set opens Finish`() {
        val (viewModel, _, _) = buildViewModel()
        viewModel.startWorkoutFromTemplate(twoExercises)
        viewModel.done()
        viewModel.done()
        assertEquals(1, viewModel.uiState.value.currentExerciseIndex)
        assertTrue(viewModel.uiState.value.lastSetLeft)

        // Plank has no target: Done is rejected until it has a time.
        viewModel.done()
        assertFalse(viewModel.uiState.value.exercises[1].sets[0].isCompleted)
        viewModel.step(SetField.Duration, plus = true)
        viewModel.done()

        val state = viewModel.uiState.value
        assertTrue(state.exercises.all { it.allDone })
        assertEquals(WorkoutDialog.Finish, state.dialog)
        // Plank has no rest of its own: the default.
        assertEquals(ActiveWorkoutViewModel.DEFAULT_REST_SECONDS, state.rest!!.seconds)
    }

    @Test
    fun `steppers stop at the floor and reject`() {
        val (viewModel, _, _) = buildViewModel()
        viewModel.startWorkoutFromTemplate(twoExercises.copy(
            templateExercises = listOf(templateExercise("ex-bench", orderIndex = 0, plannedSets = 1, plannedReps = 1))
        ))
        viewModel.drainEvents()
        viewModel.step(SetField.Reps, plus = false)
        viewModel.step(SetField.Weight, plus = false)

        val set = viewModel.uiState.value.exercises[0].sets[0]
        assertEquals("1", set.reps)
        assertEquals("0", set.weight)
        assertEquals(listOf(WorkoutEvent.Rejected), viewModel.drainEvents())
    }

    @Test
    fun `keypad Next walks weight then reps, saves typed values and refuses 0 reps`() {
        val (viewModel, _, _) = buildViewModel()
        viewModel.startWorkoutFromTemplate(twoExercises)
        viewModel.openKeypad(SetField.Weight)
        listOf(8, 2).forEach { viewModel.keypad(com.rukavina.gymbuddy.ui.design.KeypadInput.Digit(it)) }
        viewModel.keypad(com.rukavina.gymbuddy.ui.design.KeypadInput.Decimal, ',')
        viewModel.keypad(com.rukavina.gymbuddy.ui.design.KeypadInput.Digit(5))
        viewModel.keypad(com.rukavina.gymbuddy.ui.design.KeypadInput.Next)

        var state = viewModel.uiState.value
        assertEquals("82.5", state.exercises[0].sets[0].weight)
        assertEquals(SetField.Reps, state.keypad!!.field)
        assertEquals(null, viewModel.nextField(state.keypad!!))

        viewModel.keypad(com.rukavina.gymbuddy.ui.design.KeypadInput.Backspace)
        viewModel.keypad(com.rukavina.gymbuddy.ui.design.KeypadInput.Digit(0))
        assertEquals(SetRule.AtLeastOneRep, viewModel.uiState.value.keypad!!.rule)
        viewModel.drainEvents()
        viewModel.keypad(com.rukavina.gymbuddy.ui.design.KeypadInput.Next)
        assertNotNull("0 reps keeps the keypad open", viewModel.uiState.value.keypad)
        assertEquals(listOf(WorkoutEvent.Rejected), viewModel.drainEvents())

        viewModel.keypad(com.rukavina.gymbuddy.ui.design.KeypadInput.Backspace)
        viewModel.keypad(com.rukavina.gymbuddy.ui.design.KeypadInput.Digit(6))
        viewModel.keypad(com.rukavina.gymbuddy.ui.design.KeypadInput.Next)
        state = viewModel.uiState.value
        assertNull(state.keypad)
        assertEquals("6", state.exercises[0].sets[0].reps)
    }

    @Test
    fun `keypad on time takes m ss digits and Cancel keeps the old value`() {
        val (viewModel, _, _) = buildViewModel()
        viewModel.startWorkoutFromTemplate(twoExercises)
        viewModel.selectExercise(1)
        viewModel.openKeypad(SetField.Duration)
        listOf(1, 3, 0).forEach { viewModel.keypad(com.rukavina.gymbuddy.ui.design.KeypadInput.Digit(it)) }
        viewModel.keypad(com.rukavina.gymbuddy.ui.design.KeypadInput.Next)
        assertEquals("90", viewModel.uiState.value.exercises[1].sets[0].duration)

        viewModel.openKeypad(SetField.Duration)
        viewModel.keypad(com.rukavina.gymbuddy.ui.design.KeypadInput.Digit(5))
        viewModel.keypad(com.rukavina.gymbuddy.ui.design.KeypadInput.Cancel)
        assertEquals("90", viewModel.uiState.value.exercises[1].sets[0].duration)
    }

    @Test
    fun `deleting a set and undoing puts it back in place with its rest`() {
        val (viewModel, _, _) = buildViewModel()
        viewModel.startWorkoutFromTemplate(twoExercises)
        viewModel.done()
        val before = viewModel.uiState.value
        val bench = before.exercises[0]
        viewModel.drainEvents()

        viewModel.deleteSet(bench.id, bench.sets[0].id)
        var state = viewModel.uiState.value
        assertEquals(listOf(bench.sets[1].id), state.exercises[0].sets.map { it.id })
        assertEquals(1, state.exercises[0].sets[0].setNumber)
        assertNull(state.rest)
        assertEquals(listOf(WorkoutEvent.SetDeleted(bench.sets[0].id)), viewModel.drainEvents())

        viewModel.undoDelete(bench.sets[0].id)
        state = viewModel.uiState.value
        assertEquals(before.exercises, state.exercises)
        assertEquals(before.rest, state.rest)
    }

    @Test
    fun `finish saves only done sets and builds the summary`() {
        val (viewModel, sessionRepo, _) = buildViewModel()
        viewModel.startWorkoutFromTemplate(twoExercises)
        val bench = viewModel.uiState.value.exercises[0]
        viewModel.updateSetWeight(bench.id, bench.sets[0].id, "80")
        viewModel.done() // 80 × 8; set 2 now 80 × 8 too
        viewModel.step(SetField.Weight, plus = true)
        viewModel.done() // 82.5 × 8; plank left undone

        val preview = viewModel.finishPreview()
        assertEquals(2, preview.sets)
        assertEquals(1, preview.setsLeft)
        assertEquals(80f * 8 + 82.5f * 8, preview.lifted, 0.01f)

        viewModel.requestFinish()
        viewModel.saveWorkout()

        val saved = sessionRepo.created.single()
        assertEquals(listOf("ex-bench"), saved.performedExercises.map { it.exerciseId })
        val summary = viewModel.uiState.value.summary!!
        assertEquals(2, summary.sets)
        assertEquals("82.5", summary.exercises.single().top.weight)
        assertTrue("a guest is offered a backup", summary.offerBackup)
        assertNull(viewModel.uiState.value.dialog)
        assertFalse(viewModel.hasActiveWorkout())

        viewModel.dismissBackup()
        assertFalse(viewModel.uiState.value.summary!!.offerBackup)
        viewModel.closeSummary()
        assertNull(viewModel.uiState.value.summary)
    }

    @Test
    fun `an account gets no backup card`() {
        val (viewModel, _, _) = buildViewModel(owners = InMemoryDataOwnerStore("uid-1"))
        viewModel.startWorkoutFromTemplate(oneSetBench)
        viewModel.done()
        viewModel.saveWorkout()
        assertFalse(viewModel.uiState.value.summary!!.offerBackup)
    }

    @Test
    fun `a failed save sends SaveFailed`() {
        val repo = ControllableSessionRepository().apply { failing = true }
        val (viewModel, _, _) = buildViewModel(sessionRepository = repo)
        viewModel.startWorkoutFromTemplate(oneSetBench)
        viewModel.done()
        viewModel.drainEvents()
        viewModel.saveWorkout()
        assertEquals(listOf(WorkoutEvent.SaveFailed("disk full")), viewModel.drainEvents())
        assertNull(viewModel.uiState.value.summary)
    }

    @Test
    fun `rest and the current exercise survive process death`() {
        val draftRepo = FakeActiveWorkoutDraftRepository()
        val (first, _, _) = buildViewModel(draftRepo = draftRepo)
        first.startWorkoutFromTemplate(twoExercises)
        first.done()
        first.selectExercise(1)
        val before = first.uiState.value

        val (second, _, _) = buildViewModel(draftRepo = draftRepo)
        val restored = second.uiState.value
        assertEquals(1, restored.currentExerciseIndex)
        assertEquals(before.rest, restored.rest)
    }

    @Test
    fun `the hint shows until three workouts are saved`() {
        val few = FakeWorkoutSessionRepository(
            (1..2).map { com.rukavina.gymbuddy.testutil.TemplateFixtures.session("s$it", null, endedAt = 1L) }
        )
        assertTrue(buildViewModel(sessionRepo = few).first.uiState.value.showHint)
        val many = FakeWorkoutSessionRepository(
            (1..3).map { com.rukavina.gymbuddy.testutil.TemplateFixtures.session("s$it", null, endedAt = 1L) }
        )
        assertFalse(buildViewModel(sessionRepo = many).first.uiState.value.showHint)
    }

    // --- around the workout (06b) ---

    @Test
    fun `an empty workout runs with no exercises and survives process death`() {
        val draftRepo = FakeActiveWorkoutDraftRepository()
        val (first, _, _) = buildViewModel(draftRepo = draftRepo)
        first.startEmptyWorkout("Workout")

        val state = first.uiState.value
        assertTrue(state.running)
        assertTrue(first.hasActiveWorkout())
        assertTrue(state.exercises.isEmpty())
        assertNull(state.templateId)
        assertNotNull(draftRepo.draft)

        val (second, _, _) = buildViewModel(draftRepo = draftRepo)
        assertTrue(second.hasActiveWorkout())
        assertEquals("Workout", second.uiState.value.workoutTitle)
    }

    @Test
    fun `adding exercises from the picker appends them with defaults and shows the first`() {
        val (viewModel, _, _) = buildViewModel()
        viewModel.startWorkoutFromTemplate(oneSetBench)

        viewModel.addExercises(listOf("ex-plank", "unknown", "ex-bench"))

        val state = viewModel.uiState.value
        assertEquals(listOf("ex-bench", "ex-plank", "ex-bench"), state.exercises.map { it.exerciseId })
        assertEquals(1, state.currentExerciseIndex)
        val plank = state.exercises[1]
        assertEquals(3, plank.sets.size)
        // Plank is timed: no reps target. Bench gets 10 reps, no weight.
        assertTrue(plank.sets.all { it.reps.isEmpty() })
        assertTrue(state.exercises[2].sets.all { it.reps == "10" && it.weight.isEmpty() })
        assertEquals(3, state.exercises.map { it.id }.toSet().size)
    }

    @Test
    fun `adding an exercise to an empty workout makes it loggable`() {
        val (viewModel, sessionRepo, _) = buildViewModel()
        viewModel.startEmptyWorkout("Workout")
        viewModel.addExercises(listOf("ex-bench"))

        viewModel.done()
        viewModel.saveWorkout()

        val saved = sessionRepo.created.single()
        assertEquals("Workout", saved.title)
        assertNull(saved.templateId)
        assertEquals(1, saved.performedExercises.single().sets.size)
    }

    @Test
    fun `add set copies the last set's values and numbers it`() {
        val (viewModel, _, _) = buildViewModel()
        viewModel.startWorkoutFromTemplate(oneSetBench)
        viewModel.fillFirstSet("8", "60")

        viewModel.addSet()

        val sets = viewModel.uiState.value.exercises[0].sets
        assertEquals(2, sets.size)
        assertEquals(2, sets[1].setNumber)
        assertEquals("8", sets[1].reps)
        assertEquals("60", sets[1].weight)
        assertFalse(sets[1].isCompleted)
    }

    @Test
    fun `reorder moves an exercise and the shown one stays shown`() {
        val draftRepo = FakeActiveWorkoutDraftRepository()
        val (viewModel, _, _) = buildViewModel(draftRepo = draftRepo)
        viewModel.startWorkoutFromTemplate(twoExercises)
        val bench = viewModel.uiState.value.exercises[0].id

        viewModel.moveExercise(bench, 1)

        val state = viewModel.uiState.value
        assertEquals(listOf("ex-plank", "ex-bench"), state.exercises.map { it.exerciseId })
        assertEquals(1, state.currentExerciseIndex)
        assertEquals(listOf("ex-plank", "ex-bench"), draftRepo.draft!!.exercises.map { it.exerciseId })

        // Past either end: nothing happens.
        viewModel.moveExercise(bench, 1)
        viewModel.moveExercise(state.exercises[0].id, -1)
        assertEquals(state.exercises, viewModel.uiState.value.exercises)
    }

    @Test
    fun `the workout note is kept in the draft and saved as the session's notes`() {
        val draftRepo = FakeActiveWorkoutDraftRepository()
        val (first, _, _) = buildViewModel(draftRepo = draftRepo)
        first.startWorkoutFromTemplate(oneSetBench)
        first.updateNote("  shoulder was tight  ")

        val (second, sessionRepo, _) = buildViewModel(draftRepo = draftRepo)
        assertEquals("  shoulder was tight  ", second.uiState.value.note)
        second.done()
        second.saveWorkout()
        assertEquals("shoulder was tight", sessionRepo.created.single().notes)
    }

    @Test
    fun `a blank note saves no notes`() {
        val (viewModel, sessionRepo, _) = buildViewModel()
        viewModel.startWorkoutFromTemplate(oneSetBench)
        viewModel.updateNote("   ")
        viewModel.fillFirstSet("8", "60")
        viewModel.saveWorkout()
        assertNull(sessionRepo.created.single().notes)
    }

    @Test
    fun `rest timer changes this exercise's rest and the rest running after it`() {
        val (viewModel, _, _) = buildViewModel()
        viewModel.startWorkoutFromTemplate(twoExercises)
        viewModel.done()

        viewModel.setRest(150)
        viewModel.setRest(0) // ignored

        val state = viewModel.uiState.value
        assertEquals(150, state.exercises[0].restSeconds)
        assertEquals(150, state.rest!!.seconds)
        assertNull(state.exercises[1].restSeconds)

        // A rest after another exercise's set keeps its length.
        viewModel.selectExercise(1)
        viewModel.setRest(60)
        assertEquals(150, viewModel.uiState.value.rest!!.seconds)
        assertEquals(60, viewModel.uiState.value.exercises[1].restSeconds)
    }

    // --- 06c: workout service + rest buzz ---

    private fun ActiveWorkoutViewModel.awaitBuzz(on: Boolean) = kotlinx.coroutines.runBlocking {
        kotlinx.coroutines.withTimeout(2_000) { uiState.first { it.restBuzz == on } }
    }

    @Test
    fun `the service follows the running workout and stops on discard`() {
        val service = FakeWorkoutServiceStarter()
        val clock = MutableClock()
        val (viewModel, _, _) = buildViewModel(service = service, clock = clock)
        assertTrue("no workout yet: nothing pushed", service.notices.isEmpty())

        viewModel.startWorkoutFromTemplate(twoExercises)
        var notice = service.last!!
        assertEquals("Push A", notice.workoutTitle)
        assertEquals("Bench Press", notice.exerciseName)
        assertEquals(1, notice.set!!.setNumber)
        assertEquals(2, notice.setCount)
        assertFalse(notice.resting)
        assertEquals(clock.millis, notice.counterBase)

        viewModel.done()
        notice = service.last!!
        assertTrue(notice.resting)
        assertEquals(2, notice.set!!.setNumber)

        viewModel.toggleTimer()
        assertTrue(service.last!!.paused)
        assertNull(service.last!!.counterBase)

        viewModel.discardWorkout()
        assertNull(service.last)
    }

    @Test
    fun `the service stops once the workout is saved`() {
        val service = FakeWorkoutServiceStarter()
        val (viewModel, _, _) = buildViewModel(service = service)
        viewModel.startWorkoutFromTemplate(twoExercises)
        viewModel.done()
        viewModel.saveWorkout()
        assertTrue(viewModel.uiState.value.workoutSaved)
        assertNull(service.last)
    }

    @Test
    fun `buzz off never schedules`() {
        val buzz = FakeRestBuzzScheduler()
        val (viewModel, _, _) = buildViewModel(buzz = buzz)
        viewModel.startWorkoutFromTemplate(twoExercises)
        viewModel.done()
        viewModel.setRest(60)
        assertTrue(buzz.calls.none { it.startsWith("schedule") })
        assertNull(buzz.pending)
    }

    @Test
    fun `buzz follows rest start, change, undo, pause and discard`() {
        val buzz = FakeRestBuzzScheduler()
        val (viewModel, _, _) = buildViewModel(buzz = buzz)
        viewModel.setRestBuzz(true)
        viewModel.awaitBuzz(true)
        viewModel.startWorkoutFromTemplate(twoExercises)
        assertNull("no rest yet", buzz.pending)

        viewModel.done()
        assertEquals(90L, buzz.pending)

        viewModel.setRest(60)
        assertEquals(60L, buzz.pending)

        val bench = viewModel.uiState.value.exercises[0]
        viewModel.deleteSet(bench.id, bench.sets[0].id)
        assertNull("its set is gone", buzz.pending)
        viewModel.undoDelete(bench.sets[0].id)
        assertEquals(60L, buzz.pending)

        viewModel.toggleTimer()
        assertNull("paused", buzz.pending)
        viewModel.toggleTimer()
        assertEquals(60L, buzz.pending)

        viewModel.discardWorkout()
        assertNull(buzz.pending)
    }

    @Test
    fun `turning the buzz off cancels it, and no buzz once every set is done`() {
        val buzz = FakeRestBuzzScheduler()
        val (viewModel, _, _) = buildViewModel(buzz = buzz)
        viewModel.setRestBuzz(true)
        viewModel.awaitBuzz(true)
        viewModel.startWorkoutFromTemplate(twoExercises)
        viewModel.done()
        assertEquals(90L, buzz.pending)
        viewModel.setRestBuzz(false)
        viewModel.awaitBuzz(false)
        assertNull(buzz.pending)

        viewModel.setRestBuzz(true)
        viewModel.awaitBuzz(true)
        viewModel.done() // bench done, rest before the plank
        assertEquals(90L, buzz.pending)
        viewModel.step(SetField.Duration, plus = true)
        viewModel.done() // last set: nothing to rest for
        assertNull(buzz.pending)
    }
}
