package com.rukavina.gymbuddy.domain.model

/**
 * Domain model representing a single set within a performed exercise.
 * Which measurements are populated depends on the exercise's
 * ExerciseTrackingType; see domain/validation/WorkoutSetValidator.kt.
 *
 * No updatedAt/deletedAt/revision/syncState of its own - a child of the
 * WorkoutSession aggregate, governed by the parent's sync metadata.
 */
data class WorkoutSet(
    /**
     * Unique identifier for this set.
     * Use UUID string format for offline-first compatibility.
     */
    val id: String,

    /**
     * Weight used for this set in kilograms (kg).
     * Always stored in metric for consistency.
     * Convert to user's preferred display unit in the UI layer.
     * Null when not tracked for this set's exercise, or not yet entered.
     */
    val weightKg: Float? = null,

    /**
     * Number of repetitions performed in this set.
     * Null when not tracked for this set's exercise, or not yet entered.
     */
    val reps: Int? = null,

    /**
     * Duration of this set in seconds (e.g. planks, timed holds, cardio).
     * Null when not tracked for this set's exercise, or not yet entered.
     */
    val durationSeconds: Int? = null,

    /**
     * Distance covered during this set in meters (e.g. running, rowing).
     * Null when not tracked for this set's exercise, or not yet entered.
     */
    val distanceMeters: Float? = null,

    /**
     * Role this set plays within the exercise (warmup, working, drop, failure).
     */
    val setType: SetType = SetType.WORKING,

    /**
     * Whether the user has finished performing this set.
     * An in-progress set (isCompleted = false) may have every
     * measurement null - the row exists but hasn't been filled in yet.
     */
    val isCompleted: Boolean = false,

    /**
     * Rest actually taken after this set, in seconds.
     * Distinct from the template's planned rest time.
     */
    val restTakenSeconds: Int? = null,

    /**
     * Order index of this set within the exercise (0-based).
     * Used to maintain the sequence of sets.
     */
    val orderIndex: Int
)
