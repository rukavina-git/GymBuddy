package com.rukavina.gymbuddy.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.model.Equipment
import com.rukavina.gymbuddy.data.model.Exercise
import com.rukavina.gymbuddy.data.model.MuscleGroup
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import com.rukavina.gymbuddy.domain.usecase.exercise.CreateExerciseUseCase
import com.rukavina.gymbuddy.domain.usecase.exercise.DeleteExerciseUseCase
import com.rukavina.gymbuddy.domain.usecase.exercise.FilterExercisesUseCase
import com.rukavina.gymbuddy.domain.usecase.exercise.GetAllExercisesUseCase
import com.rukavina.gymbuddy.domain.usecase.exercise.GetExerciseByIdUseCase
import com.rukavina.gymbuddy.domain.usecase.exercise.SearchExercisesUseCase
import com.rukavina.gymbuddy.domain.usecase.exercise.UpdateExerciseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Exercise screen.
 * Handles business logic and exposes immutable UI state.
 * Uses use cases for clean separation of concerns.
 */
@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val getAllExercisesUseCase: GetAllExercisesUseCase,
    private val getExerciseByIdUseCase: GetExerciseByIdUseCase,
    private val createExerciseUseCase: CreateExerciseUseCase,
    private val updateExerciseUseCase: UpdateExerciseUseCase,
    private val deleteExerciseUseCase: DeleteExerciseUseCase,
    private val searchExercisesUseCase: SearchExercisesUseCase,
    private val filterExercisesUseCase: FilterExercisesUseCase,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseUiState())
    val uiState: StateFlow<ExerciseUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadExercises()
    }

    /**
     * Load all exercises from repository.
     * Automatically updates UI when data changes (Flow).
     */
    fun loadExercises() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getAllExercisesUseCase()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load exercises"
                        )
                    }
                }
                .collect { exercises ->
                    _uiState.update {
                        it.copy(
                            exercises = exercises,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    applyFilters()
                }
        }
    }

    /**
     * Search exercises by query.
     * Updates UI with filtered results.
     */
    fun searchExercises(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        if (query.isBlank()) {
            loadExercises()
            return
        }

        viewModelScope.launch {
            searchExercisesUseCase(query)
                .catch { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Search failed")
                    }
                }
                .collect { exercises ->
                    _uiState.update {
                        it.copy(exercises = exercises, errorMessage = null)
                    }
                }
        }
    }

    /**
     * Select an exercise for viewing/editing.
     */
    fun selectExercise(exerciseId: Int) {
        viewModelScope.launch {
            val exercise = getExerciseByIdUseCase(exerciseId)
            _uiState.update { it.copy(selectedExercise = exercise) }
        }
    }

    /**
     * Clear selected exercise.
     */
    fun clearSelection() {
        _uiState.update { it.copy(selectedExercise = null) }
    }

    /**
     * Create a new exercise.
     */
    fun createExercise(exercise: Exercise) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            createExerciseUseCase(exercise)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Exercise created successfully",
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to create exercise"
                        )
                    }
                }
        }
    }

    /**
     * Update an existing exercise.
     */
    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            updateExerciseUseCase(exercise)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Exercise updated successfully",
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to update exercise"
                        )
                    }
                }
        }
    }

    /**
     * Delete an exercise.
     */
    fun deleteExercise(exerciseId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            deleteExerciseUseCase(exerciseId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Exercise deleted successfully",
                            errorMessage = null,
                            selectedExercise = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to delete exercise"
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
     * Hide an exercise (only for default exercises).
     */
    fun hideExercise(exerciseId: Int) {
        viewModelScope.launch {
            try {
                exerciseRepository.hideExercise(exerciseId)
                _uiState.update {
                    it.copy(successMessage = "Exercise hidden. You can unhide it in Settings.")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to hide exercise")
                }
            }
        }
    }

    /**
     * Unhide an exercise.
     */
    fun unhideExercise(exerciseId: Int) {
        viewModelScope.launch {
            try {
                exerciseRepository.unhideExercise(exerciseId)
                _uiState.update {
                    it.copy(successMessage = "Exercise unhidden")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to unhide exercise")
                }
            }
        }
    }

    /**
     * Unhide all exercises.
     */
    fun unhideAllExercises() {
        viewModelScope.launch {
            try {
                exerciseRepository.unhideAllExercises()
                _uiState.update {
                    it.copy(successMessage = "All exercises unhidden")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to unhide exercises")
                }
            }
        }
    }

    /**
     * Update the note for an exercise.
     */
    fun updateExerciseNote(exerciseId: Int, note: String) {
        viewModelScope.launch {
            try {
                val exercise = getExerciseByIdUseCase(exerciseId)
                if (exercise != null) {
                    val updatedExercise = exercise.copy(note = note.ifBlank { null })
                    updateExerciseUseCase(updatedExercise)
                        .onSuccess {
                            _uiState.update {
                                it.copy(successMessage = "Note updated successfully")
                            }
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(errorMessage = error.message ?: "Failed to update note")
                            }
                        }
                } else {
                    _uiState.update {
                        it.copy(errorMessage = "Exercise not found")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to update note")
                }
            }
        }
    }

    /**
     * Update search query and recompute filtered exercises with debouncing.
     * Waits 300ms after user stops typing before applying filter.
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        // Cancel previous search job
        searchJob?.cancel()

        // Start new debounced search
        searchJob = viewModelScope.launch {
            delay(300) // 300ms debounce
            applyFilters()
        }
    }

    /**
     * Update selected muscle filters and recompute.
     */
    fun updateMuscleFilters(muscles: Set<MuscleGroup>) {
        _uiState.update { it.copy(selectedMuscles = muscles) }
        applyFilters()
    }

    /**
     * Update selected equipment filters and recompute.
     */
    fun updateEquipmentFilters(equipment: Set<Equipment>) {
        _uiState.update { it.copy(selectedEquipment = equipment) }
        applyFilters()
    }

    /**
     * Clear all filters (search + muscle + equipment).
     */
    fun clearAllFilters() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                selectedMuscles = emptySet(),
                selectedEquipment = emptySet()
            )
        }
        applyFilters()
    }

    /**
     * Apply all active filters to exercise list.
     * Updates filteredExercises in state.
     */
    private fun applyFilters() {
        val current = _uiState.value
        val filtered = filterExercisesUseCase(
            exercises = current.exercises,
            searchQuery = current.searchQuery,
            selectedMuscles = current.selectedMuscles,
            selectedEquipment = current.selectedEquipment
        )
        _uiState.update { it.copy(filteredExercises = filtered) }
    }

    /**
     * Flow of hidden exercises.
     */
    val hiddenExercises = exerciseRepository.getHiddenExercises()
}
