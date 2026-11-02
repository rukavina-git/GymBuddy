package com.rukavina.gymbuddy.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.domain.id.IdGenerator
import com.rukavina.gymbuddy.domain.model.ActiveWorkoutDraft
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PerformedExercise
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.repository.ActiveWorkoutDraftRepository
import com.rukavina.gymbuddy.domain.usecase.exercise.GetAllExercisesIncludingHiddenUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.CreateWorkoutSessionUseCase
import com.rukavina.gymbuddy.utils.UnitConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * Data class representing a single set within an exercise during an active workout.
 */
data class WorkoutSet(
    val id: String,
    val setNumber: Int,
    var reps: String = "",
    var weight: String = "",
    var duration: String = "",
    var distance: String = "",
    var notes: String = "",
    var isCompleted: Boolean = false
)

/**
 * Data class representing an exercise with multiple sets during an active workout.
 */
data class ActiveExercise(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val exerciseTrackingType: ExerciseTrackingType,
    val sets: List<WorkoutSet>,
    val plannedSets: Int,
    val plannedReps: Int?,
    val restSeconds: Int?
)

/**
 * UI state for the active workout screen.
 */
data class ActiveWorkoutUiState(
    val workoutTitle: String = "",
    val workoutStartTime: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val isTimerRunning: Boolean = true,
    val exercises: List<ActiveExercise> = emptyList(),
    val preferredUnits: PreferredUnits = PreferredUnits.METRIC,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val workoutSaved: Boolean = false,
    val workoutDiscarded: Boolean = false,
    val templateId: String? = null,
    val templateTitle: String? = null
)

/**
 * ViewModel for managing an active workout session.
 * Handles timer, exercise tracking, and saving the completed workout.
 *
 * Every change is written to a local draft (Room), so the workout
 * survives process death, swipe-away and reboot. A fresh ViewModel
 * restores it; a save or discard deletes it.
 */
@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val createWorkoutSessionUseCase: CreateWorkoutSessionUseCase,
    private val getAllExercisesIncludingHiddenUseCase: GetAllExercisesIncludingHiddenUseCase,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val idGenerator: IdGenerator,
    private val draftRepository: ActiveWorkoutDraftRepository,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var availableExercises: List<Exercise> = emptyList()

    /** A pending draft write. Conflated: only the newest one matters. */
    private sealed interface DraftWrite {
        data class Save(val draft: ActiveWorkoutDraft) : DraftWrite
        data object Clear : DraftWrite
    }

    private val draftWrites = MutableSharedFlow<DraftWrite>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        startDraftWriter()
        loadExercises()
        loadUserPreferences()
        restoreDraft()
    }

    /** One sequential writer, so writes land in the order they were made. */
    private fun startDraftWriter() {
        viewModelScope.launch {
            draftWrites.collect { write ->
                when (write) {
                    is DraftWrite.Save -> draftRepository.save(write.draft)
                    DraftWrite.Clear -> draftRepository.clear()
                }
            }
        }
    }

    /**
     * Resumes a workout the process lost. Skipped if a workout was
     * already started or ended in this ViewModel before the read finished.
     */
    private fun restoreDraft() {
        viewModelScope.launch {
            val draft = draftRepository.load() ?: return@launch
            val current = _uiState.value
            if (current.exercises.isNotEmpty() || current.workoutSaved || current.workoutDiscarded) {
                return@launch
            }
            // A running timer kept counting while the app was gone.
            val awaySeconds = if (draft.isTimerRunning) {
                ((clock.millis() - draft.savedAt) / 1000).coerceAtLeast(0)
            } else {
                0L
            }
            _uiState.update { state ->
                state.copy(
                    workoutTitle = draft.title,
                    workoutStartTime = draft.startedAt,
                    elapsedSeconds = draft.elapsedSeconds + awaySeconds,
                    isTimerRunning = draft.isTimerRunning,
                    exercises = draft.exercises.map { it.toActiveExercise() },
                    templateId = draft.templateId,
                    templateTitle = draft.templateTitle
                )
            }
            if (draft.isTimerRunning) startTimer()
        }
    }

    private fun persistDraft() {
        val state = _uiState.value
        if (state.exercises.isEmpty()) return
        draftWrites.tryEmit(DraftWrite.Save(state.toDraft(clock.millis())))
    }

    private fun clearDraft() {
        draftWrites.tryEmit(DraftWrite.Clear)
    }

    /**
     * Load the user's preferred display units - a device preference, not
     * account data.
     */
    private fun loadUserPreferences() {
        viewModelScope.launch {
            appPreferencesRepository.preferredUnits.collect { units ->
                _uiState.update { state -> state.copy(preferredUnits = units) }
            }
        }
    }

    /**
     * Load available exercises for name mapping.
     * Uses getAllExercisesIncludingHiddenUseCase to ensure we can map
     * exercise names even for hidden exercises.
     */
    private fun loadExercises() {
        viewModelScope.launch {
            availableExercises = getAllExercisesIncludingHiddenUseCase().first()
        }
    }

    /**
     * Start a new workout from a template.
     */
    fun startWorkoutFromTemplate(template: WorkoutTemplate) {
        viewModelScope.launch {
            // Ensure exercises are loaded before starting
            if (availableExercises.isEmpty()) {
                availableExercises = getAllExercisesIncludingHiddenUseCase().first()
            }

            val exerciseMap = availableExercises.associateBy { it.id }
            val workoutId = System.currentTimeMillis()

            val activeExercises = template.templateExercises
                .sortedBy { it.orderIndex }
                .mapIndexed { exerciseIndex, templateExercise ->
                    val exerciseName = exerciseMap[templateExercise.exerciseId]?.name ?: "Unknown Exercise"
                    val exerciseTrackingType = exerciseMap[templateExercise.exerciseId]?.trackingType
                        ?: ExerciseTrackingType.WEIGHT_REPS

                    // Create individual sets for this exercise with unique IDs.
                    // (1..plannedSets) already yields 1-based positions - do
                    // not add 1 again, or the first set displays as "Set 2".
                    val sets = (1..templateExercise.plannedSets).mapIndexed { setIndex, setNumber ->
                        WorkoutSet(
                            id = "${workoutId}_ex${exerciseIndex}_set${setIndex}",
                            setNumber = setNumber,
                            reps = "",
                            weight = "",
                            notes = ""
                        )
                    }

                    ActiveExercise(
                        id = "${workoutId}_exercise_${exerciseIndex}",
                        exerciseId = templateExercise.exerciseId,
                        exerciseName = exerciseName,
                        exerciseTrackingType = exerciseTrackingType,
                        sets = sets,
                        plannedSets = templateExercise.plannedSets,
                        plannedReps = templateExercise.plannedReps,
                        restSeconds = templateExercise.restSeconds
                    )
                }

            // Reset state completely when starting new workout, except
            // preferredUnits: that's loaded once from loadUserPreferences()'s
            // long-lived collector in init, which won't necessarily re-emit
            // just because a new workout started - resetting it here would
            // silently drop back to METRIC display/conversion for the rest
            // of the session even when the user's actual preference is
            // IMPERIAL.
            _uiState.value = ActiveWorkoutUiState(
                workoutTitle = template.title,
                workoutStartTime = System.currentTimeMillis(),
                exercises = activeExercises,
                preferredUnits = _uiState.value.preferredUnits,
                isTimerRunning = true,
                workoutSaved = false,
                isLoading = false,
                errorMessage = null,
                elapsedSeconds = 0L,
                templateId = template.id,
                templateTitle = template.title
            )

            startTimer()
            persistDraft()
        }
    }

    /**
     * Start the workout timer.
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isTimerRunning) {
                delay(1000)
                _uiState.update { state ->
                    state.copy(elapsedSeconds = state.elapsedSeconds + 1)
                }
            }
        }
    }

    /**
     * Toggle timer pause/resume.
     */
    fun toggleTimer() {
        val currentState = _uiState.value.isTimerRunning
        _uiState.update { it.copy(isTimerRunning = !currentState) }

        if (!currentState) {
            startTimer()
        } else {
            timerJob?.cancel()
        }
        persistDraft()
    }

    /**
     * Update a specific set's reps value.
     */
    fun updateSetReps(exerciseId: String, setId: String, reps: String) {
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises.map { exercise ->
                    if (exercise.id == exerciseId) {
                        exercise.copy(
                            sets = exercise.sets.map { set ->
                                if (set.id == setId) {
                                    set.copy(reps = reps.filter { it.isDigit() })
                                } else set
                            }
                        )
                    } else exercise
                }
            )
        }
        persistDraft()
    }

    /**
     * Update a specific set's weight value.
     */
    fun updateSetWeight(exerciseId: String, setId: String, weight: String) {
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises.map { exercise ->
                    if (exercise.id == exerciseId) {
                        exercise.copy(
                            sets = exercise.sets.map { set ->
                                if (set.id == setId) {
                                    set.copy(weight = weight.filter { it.isDigit() || it == '.' })
                                } else set
                            }
                        )
                    } else exercise
                }
            )
        }
        persistDraft()
    }

    /**
     * Update a specific set's duration value (whole seconds).
     */
    fun updateSetDuration(exerciseId: String, setId: String, duration: String) {
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises.map { exercise ->
                    if (exercise.id == exerciseId) {
                        exercise.copy(
                            sets = exercise.sets.map { set ->
                                if (set.id == setId) {
                                    set.copy(duration = duration.filter { it.isDigit() })
                                } else set
                            }
                        )
                    } else exercise
                }
            )
        }
        persistDraft()
    }

    /**
     * Update a specific set's distance value (meters).
     */
    fun updateSetDistance(exerciseId: String, setId: String, distance: String) {
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises.map { exercise ->
                    if (exercise.id == exerciseId) {
                        exercise.copy(
                            sets = exercise.sets.map { set ->
                                if (set.id == setId) {
                                    set.copy(distance = distance.filter { it.isDigit() || it == '.' })
                                } else set
                            }
                        )
                    } else exercise
                }
            )
        }
        persistDraft()
    }

    /**
     * Update a specific set's notes.
     */
    fun updateSetNotes(exerciseId: String, setId: String, notes: String) {
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises.map { exercise ->
                    if (exercise.id == exerciseId) {
                        exercise.copy(
                            sets = exercise.sets.map { set ->
                                if (set.id == setId) {
                                    set.copy(notes = notes)
                                } else set
                            }
                        )
                    } else exercise
                }
            )
        }
        persistDraft()
    }

    /**
     * Save the completed workout and stop the timer.
     */
    fun saveWorkout() {
        // A second tap while the first save runs would store the workout twice.
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            timerJob?.cancel()

            val state = _uiState.value

            // Convert active exercises to performed exercises with individual sets.
            // Exercises with no completed sets are dropped before assigning
            // orderIndex, so the kept exercises stay contiguously ordered.
            val performedExercises = state.exercises.mapNotNull { activeExercise ->
                val trackingType = activeExercise.exerciseTrackingType

                // Get completed sets - which fields are required depends on
                // the exercise's tracking type, mirroring WorkoutSetValidator.
                val completedSets = activeExercise.sets
                    .filter {
                        SetTrackingFields.isFilled(trackingType, it.reps, it.duration, it.distance)
                    }
                    .mapIndexed { index, uiSet ->
                        // Convert weight from display units to metric (kg) for storage
                        val weightInKg = if (SetTrackingFields.showsWeight(trackingType)) {
                            UnitConverter.weightToMetric(uiSet.weight, state.preferredUnits)
                        } else {
                            null
                        }
                        com.rukavina.gymbuddy.domain.model.WorkoutSet(
                            id = idGenerator.newId(),
                            weightKg = weightInKg,
                            reps = if (SetTrackingFields.showsReps(trackingType)) {
                                uiSet.reps.toIntOrNull()
                            } else {
                                null
                            },
                            durationSeconds = if (SetTrackingFields.showsDuration(trackingType)) {
                                uiSet.duration.toIntOrNull()
                            } else {
                                null
                            },
                            distanceMeters = if (SetTrackingFields.showsDistance(trackingType)) {
                                uiSet.distance.toFloatOrNull()
                            } else {
                                null
                            },
                            isCompleted = true,
                            orderIndex = index
                        )
                    }

                if (completedSets.isEmpty()) return@mapNotNull null

                Triple(activeExercise.exerciseId, completedSets, trackingType)
            }.mapIndexed { index, (exerciseId, completedSets, trackingType) ->
                // exerciseName/exerciseCategory/exercisePrimaryMuscles are
                // placeholders here - ValidateWorkoutSessionSetsUseCase
                // resolves the real Exercise and stamps the actual snapshot
                // before this is persisted. exerciseTrackingType is already
                // the real, resolved value from the active exercise.
                PerformedExercise(
                    id = idGenerator.newId(),
                    exerciseId = exerciseId,
                    orderIndex = index,
                    exerciseName = "",
                    exerciseCategory = ExerciseCategory.STRENGTH,
                    exerciseTrackingType = trackingType,
                    exercisePrimaryMuscles = emptyList(),
                    sets = completedSets
                )
            }

            // Validate that there are exercises logged. This is a rejected
            // stop attempt, not a discard - the workout is still in progress,
            // so resume the timer cancelled above and keep the user here to
            // add a set or discard explicitly. Only an explicit discard or a
            // successful save should navigate away.
            if (performedExercises.isEmpty()) {
                startTimer()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No exercises logged. Workout not saved."
                    )
                }
                return@launch
            }

            val workoutSession = WorkoutSession(
                id = idGenerator.newId(),
                startedAt = state.workoutStartTime,
                endedAt = System.currentTimeMillis(),
                durationSeconds = state.elapsedSeconds.toInt(), // Store total seconds
                title = state.workoutTitle.ifBlank { "Workout" },
                templateId = state.templateId,
                templateTitle = state.templateTitle,
                performedExercises = performedExercises
            )

            createWorkoutSessionUseCase(workoutSession)
                .onSuccess {
                    clearDraft()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            workoutSaved = true,
                            isTimerRunning = false,
                            exercises = emptyList(),
                            elapsedSeconds = 0L,
                            workoutTitle = ""
                        )
                    }
                }
                .onFailure { error ->
                    // Save failed: this is a rejected stop attempt too, so the
                    // same treatment applies. Resume the timer cancelled above
                    // and leave exercises/elapsedSeconds/workoutTitle intact -
                    // neither workoutDiscarded nor workoutSaved is set here, so
                    // the user stays on this screen with their data and can
                    // retry or fix the offending set.
                    startTimer()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to save workout"
                        )
                    }
                }
        }
    }

    /**
     * Discard the current workout without saving.
     */
    fun discardWorkout() {
        clearDraft()
        timerJob?.cancel()
        timerJob = null
        _uiState.value = ActiveWorkoutUiState(
            workoutDiscarded = true,
            isTimerRunning = false,
            exercises = emptyList(),
            elapsedSeconds = 0L,
            // See startWorkoutFromTemplate - not preserving this would
            // silently drop back to METRIC until the next real preference
            // change re-emits from loadUserPreferences()'s collector.
            preferredUnits = _uiState.value.preferredUnits
        )
    }

    /**
     * Check if there's an active workout in progress.
     */
    fun hasActiveWorkout(): Boolean {
        return _uiState.value.exercises.isNotEmpty() && !_uiState.value.workoutSaved
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

private fun ActiveWorkoutUiState.toDraft(now: Long) = ActiveWorkoutDraft(
    title = workoutTitle,
    templateId = templateId,
    templateTitle = templateTitle,
    startedAt = workoutStartTime,
    elapsedSeconds = elapsedSeconds,
    isTimerRunning = isTimerRunning,
    savedAt = now,
    exercises = exercises.map { exercise ->
        ActiveWorkoutDraft.Exercise(
            id = exercise.id,
            exerciseId = exercise.exerciseId,
            exerciseName = exercise.exerciseName,
            trackingType = exercise.exerciseTrackingType,
            plannedSets = exercise.plannedSets,
            plannedReps = exercise.plannedReps,
            restSeconds = exercise.restSeconds,
            sets = exercise.sets.map { set ->
                ActiveWorkoutDraft.Set(
                    id = set.id,
                    setNumber = set.setNumber,
                    reps = set.reps,
                    weight = set.weight,
                    duration = set.duration,
                    distance = set.distance,
                    notes = set.notes,
                    isCompleted = set.isCompleted
                )
            }
        )
    }
)

private fun ActiveWorkoutDraft.Exercise.toActiveExercise() = ActiveExercise(
    id = id,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    exerciseTrackingType = trackingType,
    sets = sets.map { set ->
        WorkoutSet(
            id = set.id,
            setNumber = set.setNumber,
            reps = set.reps,
            weight = set.weight,
            duration = set.duration,
            distance = set.distance,
            notes = set.notes,
            isCompleted = set.isCompleted
        )
    },
    plannedSets = plannedSets,
    plannedReps = plannedReps,
    restSeconds = restSeconds
)
