package com.rukavina.gymbuddy.ui.exercise

import com.rukavina.gymbuddy.data.model.Equipment
import com.rukavina.gymbuddy.data.model.Exercise
import com.rukavina.gymbuddy.data.model.MuscleGroup

/**
 * UI state for Exercise screen.
 * Immutable state that represents the current UI.
 */
data class ExerciseUiState(
    /**
     * List of all exercises to display.
     */
    val exercises: List<Exercise> = emptyList(),

    /**
     * Currently selected exercise for viewing/editing.
     * Null if no exercise is selected.
     */
    val selectedExercise: Exercise? = null,

    /**
     * Search query for filtering exercises.
     */
    val searchQuery: String = "",

    /**
     * Selected muscle groups for filtering.
     * Empty set means no muscle filter applied.
     */
    val selectedMuscles: Set<MuscleGroup> = emptySet(),

    /**
     * Selected equipment for filtering.
     * Empty set means no equipment filter applied.
     */
    val selectedEquipment: Set<Equipment> = emptySet(),

    /**
     * Filtered exercises based on search query and selected filters.
     * Computed in ViewModel using FilterExercisesUseCase.
     */
    val filteredExercises: List<Exercise> = emptyList(),

    /**
     * Loading state - true when fetching data.
     */
    val isLoading: Boolean = false,

    /**
     * Error message to display to user.
     * Null if no error.
     */
    val errorMessage: String? = null,

    /**
     * Success message after operations (create, update, delete).
     * Null if no success message to show.
     */
    val successMessage: String? = null
)
