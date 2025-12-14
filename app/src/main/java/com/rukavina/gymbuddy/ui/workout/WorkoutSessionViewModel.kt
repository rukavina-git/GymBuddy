package com.rukavina.gymbuddy.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.model.PerformedExercise
import com.rukavina.gymbuddy.data.model.WorkoutSession
import com.rukavina.gymbuddy.data.model.WorkoutTemplate
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
import java.util.UUID
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
    private val deleteWorkoutSessionUseCase: DeleteWorkoutSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutSessionUiState())
    val uiState: StateFlow<WorkoutSessionUiState> = _uiState.asStateFlow()

    init {
        loadWorkoutSessions()
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
                    _uiState.update {
                        it.copy(
                            workoutSessions = workoutSessions,
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
                    _uiState.update {
                        it.copy(
                            workoutSessions = workoutSessions,
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
     * Create a new workout session.
     */
    fun createWorkoutSession(workoutSession: WorkoutSession) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
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
     * Update an existing workout session.
     */
    fun updateWorkoutSession(workoutSession: WorkoutSession) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
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
                .map { templateExercise ->
                    // Create empty sets that user will fill in during workout
                    val sets = List(templateExercise.plannedSets) { index ->
                        com.rukavina.gymbuddy.data.model.WorkoutSet(
                            id = UUID.randomUUID().toString(),
                            weight = 0f, // User will fill in
                            reps = templateExercise.plannedReps,
                            orderIndex = index
                        )
                    }

                    PerformedExercise(
                        id = UUID.randomUUID().toString(),
                        exerciseId = templateExercise.exerciseId,
                        sets = sets
                    )
                }

            val newSession = WorkoutSession(
                id = UUID.randomUUID().toString(),
                date = System.currentTimeMillis(),
                durationSeconds = 0, // Will be updated when session ends
                title = template.title, // Use template title as default title
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
