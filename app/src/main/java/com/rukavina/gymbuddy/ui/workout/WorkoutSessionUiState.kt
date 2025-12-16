package com.rukavina.gymbuddy.ui.workout

import com.rukavina.gymbuddy.data.model.PreferredUnits
import com.rukavina.gymbuddy.data.model.WorkoutSession

/**
 * UI state for WorkoutSession screen.
 * Immutable state that represents the current UI.
 */
data class WorkoutSessionUiState(
    /**
     * List of all workout sessions to display.
     * Ordered by date (most recent first).
     */
    val workoutSessions: List<WorkoutSession> = emptyList(),

    /**
     * Currently selected workout session for viewing/editing.
     * Null if no workout session is selected.
     */
    val selectedWorkoutSession: WorkoutSession? = null,

    /**
     * User's preferred units for display
     */
    val preferredUnits: PreferredUnits = PreferredUnits.METRIC,

    /**
     * Date range filter - start date in milliseconds.
     * Null if no filter applied.
     */
    val filterStartDate: Long? = null,

    /**
     * Date range filter - end date in milliseconds.
     * Null if no filter applied.
     */
    val filterEndDate: Long? = null,

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
