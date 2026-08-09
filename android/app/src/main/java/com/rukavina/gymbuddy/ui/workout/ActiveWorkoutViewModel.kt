package com.rukavina.gymbuddy.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.rukavina.gymbuddy.domain.id.IdGenerator
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PerformedExercise
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.domain.usecase.exercise.GetAllExercisesIncludingHiddenUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.CreateWorkoutSessionUseCase
import com.rukavina.gymbuddy.utils.UnitConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data class representing a single set within an exercise during an active workout.
 */
data class WorkoutSet(
    val id: String,
    val setNumber: Int,
    var reps: String = "",
    var weight: String = "",
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
    val sets: List<WorkoutSet>,
    val plannedSets: Int,
    val plannedReps: Int
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
    val workoutDiscarded: Boolean = false
)

/**
 * ViewModel for managing an active workout session.
 * Handles timer, exercise tracking, and saving the completed workout.
 */
@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val createWorkoutSessionUseCase: CreateWorkoutSessionUseCase,
    private val getAllExercisesIncludingHiddenUseCase: GetAllExercisesIncludingHiddenUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val idGenerator: IdGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var availableExercises: List<Exercise> = emptyList()

    init {
        loadExercises()
        loadUserPreferences()
    }

    /**
     * Load user's preferred units from their profile.
     */
    private fun loadUserPreferences() {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            uid?.let {
                val profile = userProfileRepository.getProfile(it)
                profile?.let { p ->
                    _uiState.update { state ->
                        state.copy(preferredUnits = p.preferredUnits)
                    }
                }
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

                    // Create individual sets for this exercise with unique IDs
                    val sets = (1..templateExercise.plannedSets).mapIndexed { setIndex, setNumber ->
                        WorkoutSet(
                            id = "${workoutId}_ex${exerciseIndex}_set${setIndex}",
                            setNumber = setNumber + 1,
                            reps = "",
                            weight = "",
                            notes = ""
                        )
                    }

                    ActiveExercise(
                        id = "${workoutId}_exercise_${exerciseIndex}",
                        exerciseId = templateExercise.exerciseId,
                        exerciseName = exerciseName,
                        sets = sets,
                        plannedSets = templateExercise.plannedSets,
                        plannedReps = templateExercise.plannedReps
                    )
                }

            // Reset state completely when starting new workout
            _uiState.value = ActiveWorkoutUiState(
                workoutTitle = template.title,
                workoutStartTime = System.currentTimeMillis(),
                exercises = activeExercises,
                isTimerRunning = true,
                workoutSaved = false,
                isLoading = false,
                errorMessage = null,
                elapsedSeconds = 0L
            )

            startTimer()
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
    }

    /**
     * Save the completed workout and stop the timer.
     */
    fun saveWorkout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            timerJob?.cancel()

            val state = _uiState.value

            // Convert active exercises to performed exercises with individual sets.
            // Exercises with no completed sets are dropped before assigning
            // orderIndex, so the kept exercises stay contiguously ordered.
            val performedExercises = state.exercises.mapNotNull { activeExercise ->
                // Get completed sets
                val completedSets = activeExercise.sets
                    .filter { it.reps.isNotBlank() && it.weight.isNotBlank() }
                    .mapIndexed { index, uiSet ->
                        // Convert weight from display units to metric (kg) for storage
                        val weightInKg = UnitConverter.weightToMetric(uiSet.weight, state.preferredUnits)
                        com.rukavina.gymbuddy.domain.model.WorkoutSet(
                            id = idGenerator.newId(),
                            weightKg = weightInKg,
                            reps = uiSet.reps.toIntOrNull(),
                            isCompleted = true,
                            orderIndex = index
                        )
                    }

                if (completedSets.isEmpty()) return@mapNotNull null

                activeExercise.exerciseId to completedSets
            }.mapIndexed { index, (exerciseId, completedSets) ->
                // exerciseName/exerciseCategory/exerciseTrackingType/exercisePrimaryMuscles
                // are placeholders here - ValidateWorkoutSessionSetsUseCase resolves the
                // real Exercise and stamps the actual snapshot before this is persisted.
                PerformedExercise(
                    id = idGenerator.newId(),
                    exerciseId = exerciseId,
                    orderIndex = index,
                    exerciseName = "",
                    exerciseCategory = ExerciseCategory.STRENGTH,
                    exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
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
                date = state.workoutStartTime,
                durationSeconds = state.elapsedSeconds.toInt(), // Store total seconds
                title = state.workoutTitle.ifBlank { "Workout" },
                performedExercises = performedExercises
            )

            createWorkoutSessionUseCase(workoutSession)
                .onSuccess {
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
        timerJob?.cancel()
        timerJob = null
        _uiState.value = ActiveWorkoutUiState(
            workoutDiscarded = true,
            isTimerRunning = false,
            exercises = emptyList(),
            elapsedSeconds = 0L
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
