package com.rukavina.gymbuddy.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.domain.id.IdGenerator
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PerformedExercise
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.model.WorkoutSet
import com.rukavina.gymbuddy.domain.usecase.workout.CreateWorkoutSessionUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.DeleteWorkoutSessionUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.GetAllWorkoutSessionsUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.GetWorkoutSessionByIdUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.GetWorkoutSessionsByDateRangeUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.UpdateWorkoutSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for WorkoutSession screen.
 * Handles business logic and exposes immutable UI state.
 * Uses use cases for clean separation of concerns.
 */
@HiltViewModel
class WorkoutSessionViewModel @Inject constructor(
    private val getAllWorkoutSessionsUseCase: GetAllWorkoutSessionsUseCase,
    private val getWorkoutSessionByIdUseCase: GetWorkoutSessionByIdUseCase,
    private val getWorkoutSessionsByDateRangeUseCase: GetWorkoutSessionsByDateRangeUseCase,
    private val createWorkoutSessionUseCase: CreateWorkoutSessionUseCase,
    private val updateWorkoutSessionUseCase: UpdateWorkoutSessionUseCase,
    private val deleteWorkoutSessionUseCase: DeleteWorkoutSessionUseCase,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val idGenerator: IdGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutSessionUiState())
    val uiState: StateFlow<WorkoutSessionUiState> = _uiState.asStateFlow()

    init {
        loadWorkoutSessions()
        loadUserPreferences()
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
     * Load all workout sessions from repository.
     * Automatically updates UI when data changes (Flow).
     */
    fun loadWorkoutSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getAllWorkoutSessionsUseCase()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load workout sessions"
                        )
                    }
                }
                .collect { workoutSessions ->
                    _uiState.update { state ->
                        state.copy(
                            workoutSessions = applySorting(workoutSessions, state.sortOrder),
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    /**
     * Filter workout sessions by date range.
     * @param startDate Unix timestamp in milliseconds
     * @param endDate Unix timestamp in milliseconds
     */
    fun filterWorkoutSessionsByDateRange(startDate: Long, endDate: Long) {
        _uiState.update {
            it.copy(
                filterStartDate = startDate,
                filterEndDate = endDate,
                isLoading = true
            )
        }

        viewModelScope.launch {
            getWorkoutSessionsByDateRangeUseCase(startDate, endDate)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to filter workout sessions"
                        )
                    }
                }
                .collect { workoutSessions ->
                    _uiState.update { state ->
                        state.copy(
                            workoutSessions = applySorting(workoutSessions, state.sortOrder),
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    /**
     * Clear date range filter and show all workout sessions.
     */
    fun clearDateFilter() {
        _uiState.update {
            it.copy(
                filterStartDate = null,
                filterEndDate = null
            )
        }
        loadWorkoutSessions()
    }

    /**
     * Select a workout session for viewing/editing.
     */
    fun selectWorkoutSession(workoutSessionId: String) {
        viewModelScope.launch {
            val workoutSession = getWorkoutSessionByIdUseCase(workoutSessionId)
            _uiState.update { it.copy(selectedWorkoutSession = workoutSession) }
        }
    }

    /**
     * Clear selected workout session.
     */
    fun clearSelection() {
        _uiState.update { it.copy(selectedWorkoutSession = null) }
    }

    /**
     * Create a new workout session from user input collected by
     * WorkoutSessionFormDialog. Mints every id with IdGenerator - the
     * dialog only ever relays ids that already existed.
     */
    fun createWorkoutSession(draft: WorkoutSessionDraft) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val workoutSession = buildWorkoutSession(existingId = null, draft = draft)
            createWorkoutSessionUseCase(workoutSession)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Workout session created successfully",
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to create workout session"
                        )
                    }
                }
        }
    }

    /**
     * Update an existing workout session from user input collected by
     * WorkoutSessionFormDialog. Preserves the session's own id and every
     * existingId the draft carries forward; mints fresh ids for anything
     * added during this edit.
     */
    fun updateWorkoutSession(existing: WorkoutSession, draft: WorkoutSessionDraft) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val workoutSession = buildWorkoutSession(existingId = existing.id, draft = draft)
            updateWorkoutSessionUseCase(workoutSession)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Workout session updated successfully",
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to update workout session"
                        )
                    }
                }
        }
    }

    /**
     * Construct the real WorkoutSession/PerformedExercise/WorkoutSet
     * entities from user input, minting an id for anything the draft
     * didn't already have. This is the only place in this flow that mints
     * domain identity - WorkoutScreen's dialogs only ever relay ids they
     * were already given.
     */
    private fun buildWorkoutSession(existingId: String?, draft: WorkoutSessionDraft): WorkoutSession {
        val performedExercises = draft.performedExercises.map { peDraft ->
            // exerciseName is the resolved display name from the dialog;
            // exerciseCategory/exercisePrimaryMuscles are placeholders -
            // ValidateWorkoutSessionSetsUseCase resolves the real Exercise
            // and stamps the actual snapshot before this is persisted.
            PerformedExercise(
                id = peDraft.existingId ?: idGenerator.newId(),
                exerciseId = peDraft.exerciseId,
                orderIndex = peDraft.orderIndex,
                exerciseName = peDraft.exerciseName,
                exerciseCategory = ExerciseCategory.STRENGTH,
                exerciseTrackingType = peDraft.exerciseTrackingType,
                exercisePrimaryMuscles = emptyList(),
                sets = peDraft.sets.map { setDraft ->
                    WorkoutSet(
                        id = setDraft.existingId ?: idGenerator.newId(),
                        weightKg = setDraft.weightKg,
                        reps = setDraft.reps,
                        durationSeconds = setDraft.durationSeconds,
                        distanceMeters = setDraft.distanceMeters,
                        isCompleted = true,
                        orderIndex = setDraft.orderIndex
                    )
                }
            )
        }

        return WorkoutSession(
            id = existingId ?: idGenerator.newId(),
            startedAt = draft.startedAt,
            endedAt = draft.endedAt,
            durationSeconds = draft.durationSeconds,
            title = draft.title,
            notes = draft.notes,
            templateId = draft.templateId,
            templateTitle = draft.templateTitle,
            performedExercises = performedExercises
        )
    }

    /**
     * Delete a workout session.
     */
    fun deleteWorkoutSession(workoutSessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            deleteWorkoutSessionUseCase(workoutSessionId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Workout session deleted successfully",
                            errorMessage = null,
                            selectedWorkoutSession = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to delete workout session"
                        )
                    }
                }
        }
    }

    /**
     * Clear error message after user has seen it.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Clear success message after user has seen it.
     */
    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * Change the sort order for workout sessions.
     */
    fun setSortOrder(sortOrder: WorkoutSessionSortOrder) {
        _uiState.update { state ->
            state.copy(
                sortOrder = sortOrder,
                workoutSessions = applySorting(state.workoutSessions, sortOrder)
            )
        }
    }

    /**
     * Apply sorting to a list of workout sessions based on the sort order.
     */
    private fun applySorting(
        sessions: List<WorkoutSession>,
        sortOrder: WorkoutSessionSortOrder
    ): List<WorkoutSession> {
        return when (sortOrder) {
            WorkoutSessionSortOrder.DATE_NEWEST_FIRST -> sessions.sortedByDescending { it.startedAt }
            WorkoutSessionSortOrder.DATE_OLDEST_FIRST -> sessions.sortedBy { it.startedAt }
            WorkoutSessionSortOrder.DURATION_LONGEST_FIRST -> sessions.sortedByDescending { it.durationSeconds }
            WorkoutSessionSortOrder.DURATION_SHORTEST_FIRST -> sessions.sortedBy { it.durationSeconds }
            WorkoutSessionSortOrder.TITLE_A_TO_Z -> sessions.sortedBy { it.title?.lowercase() ?: "" }
            WorkoutSessionSortOrder.TITLE_Z_TO_A -> sessions.sortedByDescending { it.title?.lowercase() ?: "" }
        }
    }

    /**
     * Start a new workout session from a template.
     * Creates a WorkoutSession with PerformedExercises pre-populated from the template.
     * Weight, sets, and reps are initially set from template but can be edited during workout.
     */
    fun startSessionFromTemplate(template: WorkoutTemplate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Convert TemplateExercises to PerformedExercises with sets
            val performedExercises = template.templateExercises
                .sortedBy { it.orderIndex }
                .mapIndexed { index, templateExercise ->
                    // Create empty sets that user will fill in during workout
                    val sets = List(templateExercise.plannedSets) { setIndex ->
                        WorkoutSet(
                            id = idGenerator.newId(),
                            weightKg = 0f, // User will fill in
                            reps = templateExercise.plannedReps,
                            orderIndex = setIndex
                        )
                    }

                    // exerciseName/exerciseCategory/exerciseTrackingType/exercisePrimaryMuscles
                    // are placeholders here - ValidateWorkoutSessionSetsUseCase resolves the
                    // real Exercise and stamps the actual snapshot before this is persisted.
                    PerformedExercise(
                        id = idGenerator.newId(),
                        exerciseId = templateExercise.exerciseId,
                        orderIndex = index,
                        exerciseName = "",
                        exerciseCategory = ExerciseCategory.STRENGTH,
                        exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
                        exercisePrimaryMuscles = emptyList(),
                        sets = sets
                    )
                }

            val newSession = WorkoutSession(
                id = idGenerator.newId(),
                startedAt = System.currentTimeMillis(),
                durationSeconds = 0, // Will be updated when session ends
                title = template.title, // Use template title as default title
                templateId = template.id,
                templateTitle = template.title,
                performedExercises = performedExercises
            )

            createWorkoutSessionUseCase(newSession)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedWorkoutSession = newSession,
                            successMessage = "Workout session started from template"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to start workout session"
                        )
                    }
                }
        }
    }
}
