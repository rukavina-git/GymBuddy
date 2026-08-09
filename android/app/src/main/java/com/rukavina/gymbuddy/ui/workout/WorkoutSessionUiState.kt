package com.rukavina.gymbuddy.ui.workout

import com.rukavina.gymbuddy.data.model.PreferredUnits
import com.rukavina.gymbuddy.data.model.WorkoutSession

/**
 * Sort order options for workout sessions.
 */
enum class WorkoutSessionSortOrder {
    DATE_NEWEST_FIRST,
    DATE_OLDEST_FIRST,
    DURATION_LONGEST_FIRST,
    DURATION_SHORTEST_FIRST,
    TITLE_A_TO_Z,
    TITLE_Z_TO_A
}

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
    val successMessage: String? = null,

    /**
     * Current sort order for workout sessions.
     * Default is most recent first.
     */
    val sortOrder: WorkoutSessionSortOrder = WorkoutSessionSortOrder.DATE_NEWEST_FIRST
)
