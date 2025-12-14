package com.rukavina.gymbuddy.ui.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.usecase.exercise.GetAllExercisesUseCase
import com.rukavina.gymbuddy.domain.usecase.template.CreateWorkoutTemplateUseCase
import com.rukavina.gymbuddy.domain.usecase.template.DeleteWorkoutTemplateUseCase
import com.rukavina.gymbuddy.domain.usecase.template.GetAllWorkoutTemplatesUseCase
import com.rukavina.gymbuddy.domain.usecase.template.GetWorkoutTemplateByIdUseCase
import com.rukavina.gymbuddy.domain.usecase.template.SearchWorkoutTemplatesUseCase
import com.rukavina.gymbuddy.domain.usecase.template.UpdateWorkoutTemplateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for WorkoutTemplate screen.
 * Handles business logic and exposes immutable UI state.
 * Uses use cases for clean separation of concerns.
 */
@HiltViewModel
class WorkoutTemplateViewModel @Inject constructor(
    private val getAllWorkoutTemplatesUseCase: GetAllWorkoutTemplatesUseCase,
    private val getWorkoutTemplateByIdUseCase: GetWorkoutTemplateByIdUseCase,
    private val searchWorkoutTemplatesUseCase: SearchWorkoutTemplatesUseCase,
    private val createWorkoutTemplateUseCase: CreateWorkoutTemplateUseCase,
    private val updateWorkoutTemplateUseCase: UpdateWorkoutTemplateUseCase,
    private val deleteWorkoutTemplateUseCase: DeleteWorkoutTemplateUseCase,
    private val getAllExercisesUseCase: GetAllExercisesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutTemplateUiState())
    val uiState: StateFlow<WorkoutTemplateUiState> = _uiState.asStateFlow()

    init {
        loadTemplates()
        loadAvailableExercises()
    }

    /**
     * Load all workout templates from repository.
     * Automatically updates UI when data changes (Flow).
     */
    fun loadTemplates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, searchQuery = "") }
            getAllWorkoutTemplatesUseCase()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load workout templates"
                        )
                    }
                }
                .collect { templates ->
                    _uiState.update {
                        it.copy(
                            templates = templates,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    /**
     * Load all available exercises for template creation/editing.
     * Populates dropdown/picker UI components.
     */
    private fun loadAvailableExercises() {
        viewModelScope.launch {
            getAllExercisesUseCase()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: "Failed to load exercises"
                        )
                    }
                }
                .collect { exercises ->
                    _uiState.update {
                        it.copy(availableExercises = exercises)
                    }
                }
        }
    }

    /**
     * Search workout templates by query string (case-insensitive).
     * @param query Search query to filter templates by title
     */
    fun searchTemplates(query: String) {
        _uiState.update {
            it.copy(searchQuery = query, isLoading = true)
        }

        viewModelScope.launch {
            searchWorkoutTemplatesUseCase(query)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to search workout templates"
                        )
                    }
                }
                .collect { templates ->
                    _uiState.update {
                        it.copy(
                            templates = templates,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    /**
     * Select a workout template for viewing/editing.
     */
    fun selectTemplate(templateId: String) {
        viewModelScope.launch {
            val template = getWorkoutTemplateByIdUseCase(templateId)
            _uiState.update { it.copy(selectedTemplate = template) }
        }
    }

    /**
     * Clear selected workout template.
     */
    fun clearSelection() {
        _uiState.update { it.copy(selectedTemplate = null) }
    }

    /**
     * Create a new workout template.
     */
    fun createTemplate(template: WorkoutTemplate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            createWorkoutTemplateUseCase(template)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Workout template created successfully",
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to create workout template"
                        )
                    }
                }
        }
    }

    /**
     * Update an existing workout template.
     */
    fun updateTemplate(template: WorkoutTemplate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            updateWorkoutTemplateUseCase(template)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Workout template updated successfully",
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to update workout template"
                        )
                    }
                }
        }
    }

    /**
     * Delete a workout template.
     */
    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            deleteWorkoutTemplateUseCase(templateId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Workout template deleted successfully",
                            errorMessage = null,
                            selectedTemplate = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to delete workout template"
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
}
