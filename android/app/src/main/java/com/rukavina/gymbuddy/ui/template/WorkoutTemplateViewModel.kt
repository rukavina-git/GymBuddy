package com.rukavina.gymbuddy.ui.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.domain.id.IdGenerator
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.TemplateExercise
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.usecase.exercise.GetAllExercisesUseCase
import com.rukavina.gymbuddy.domain.usecase.exercise.GetAllExercisesIncludingHiddenUseCase
import com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository
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
    private val getAllExercisesUseCase: GetAllExercisesUseCase,
    private val getAllExercisesIncludingHiddenUseCase: GetAllExercisesIncludingHiddenUseCase,
    private val workoutTemplateRepository: WorkoutTemplateRepository,
    private val idGenerator: IdGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutTemplateUiState())
    val uiState: StateFlow<WorkoutTemplateUiState> = _uiState.asStateFlow()

    val hiddenTemplates = workoutTemplateRepository.getHiddenTemplates()

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
     * Uses getAllExercisesIncludingHiddenUseCase to ensure we can look up
     * exercise names even for hidden exercises (important for template display).
     */
    private fun loadAvailableExercises() {
        viewModelScope.launch {
            getAllExercisesIncludingHiddenUseCase()
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
     * Create a new workout template from user input collected by
     * WorkoutTemplateFormDialog. Mints every id with IdGenerator - the
     * dialog only ever relays ids that already existed.
     */
    fun createTemplate(draft: WorkoutTemplateDraft) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val template = buildTemplate(existing = null, draft = draft)
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
     * Update an existing workout template from user input collected by
     * WorkoutTemplateFormDialog. Preserves the template's own id, source,
     * ownerId and derivedFromId, and every existingId the draft carries
     * forward for its exercises; mints fresh ids for anything added during
     * this edit.
     */
    fun updateTemplate(existing: WorkoutTemplate, draft: WorkoutTemplateDraft) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val template = buildTemplate(existing = existing, draft = draft)
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
     * Construct the real WorkoutTemplate/TemplateExercise entities from
     * user input, minting an id for anything the draft didn't already
     * have. This is the only place in this flow that mints domain
     * identity - WorkoutTemplateScreen's dialogs only ever relay ids they
     * were already given.
     */
    private fun buildTemplate(existing: WorkoutTemplate?, draft: WorkoutTemplateDraft): WorkoutTemplate {
        val exercises = draft.exercises.map { exerciseDraft ->
            TemplateExercise(
                id = exerciseDraft.existingId ?: idGenerator.newId(),
                exerciseId = exerciseDraft.exerciseId,
                exerciseName = exerciseDraft.exerciseName,
                exerciseTrackingType = exerciseDraft.exerciseTrackingType,
                plannedSets = exerciseDraft.plannedSets,
                plannedReps = exerciseDraft.plannedReps,
                orderIndex = exerciseDraft.orderIndex,
                restSeconds = exerciseDraft.restSeconds,
                notes = exerciseDraft.notes
            )
        }

        return WorkoutTemplate(
            id = existing?.id ?: idGenerator.newId(),
            title = draft.title,
            templateExercises = exercises,
            source = existing?.source ?: EntitySource.CUSTOM,
            ownerId = existing?.ownerId,
            derivedFromId = existing?.derivedFromId,
            deprecated = existing?.deprecated ?: false
        )
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

    /**
     * Hide a default workout template.
     * Hidden templates won't appear in the main list.
     */
    fun hideTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                workoutTemplateRepository.hideTemplate(templateId)
                _uiState.update {
                    it.copy(successMessage = "Template hidden")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to hide template")
                }
            }
        }
    }

    /**
     * Unhide a template.
     * Makes the template visible again in the main list.
     */
    fun unhideTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                workoutTemplateRepository.unhideTemplate(templateId)
                _uiState.update {
                    it.copy(successMessage = "Template restored")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to unhide template")
                }
            }
        }
    }

    /**
     * Unhide all hidden templates.
     */
    fun unhideAllTemplates() {
        viewModelScope.launch {
            try {
                // Get all hidden templates and unhide them one by one
                hiddenTemplates.collect { templates ->
                    templates.forEach { template ->
                        workoutTemplateRepository.unhideTemplate(template.id)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to unhide templates")
                }
            }
        }
    }
}
