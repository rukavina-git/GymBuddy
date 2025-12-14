package com.rukavina.gymbuddy.ui.exercise

import com.rukavina.gymbuddy.data.model.Exercise

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
