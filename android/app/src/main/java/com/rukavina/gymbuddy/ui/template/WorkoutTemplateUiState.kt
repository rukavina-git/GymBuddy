package com.rukavina.gymbuddy.ui.template

import com.rukavina.gymbuddy.data.model.Exercise
import com.rukavina.gymbuddy.data.model.WorkoutTemplate

/**
 * UI state for WorkoutTemplate screen.
 * Immutable state that represents the current UI.
 *
 * Follows the same pattern as WorkoutSessionUiState for consistency.
 */
data class WorkoutTemplateUiState(
    /**
     * List of all workout templates to display.
     * Ordered alphabetically by title.
     */
    val templates: List<WorkoutTemplate> = emptyList(),

    /**
     * Currently selected template for viewing/editing.
     * Null if no template is selected.
     */
    val selectedTemplate: WorkoutTemplate? = null,

    /**
     * List of available exercises for selection when creating/editing templates.
     * Used in dropdown/picker UI components to select exercises.
     */
    val availableExercises: List<Exercise> = emptyList(),

    /**
     * Search query for filtering templates by title.
     * Empty string means no filter applied.
     */
    val searchQuery: String = "",

    /**
     * Loading state - true when fetching data.
     * Used to show loading indicators in UI.
     */
    val isLoading: Boolean = false,

    /**
     * Error message to display to user.
     * Null if no error.
     * Should be cleared after user acknowledges.
     */
    val errorMessage: String? = null,

    /**
     * Success message after operations (create, update, delete).
     * Null if no success message to show.
     * Should be cleared after brief display.
     */
    val successMessage: String? = null
)
