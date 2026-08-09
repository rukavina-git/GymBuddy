package com.rukavina.gymbuddy.data.model

/**
 * Domain model representing a single set within a performed exercise.
 * Each set can have different weight and reps.
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
     */
    val weight: Float,

    /**
     * Number of repetitions performed in this set.
     */
    val reps: Int,

    /**
     * Order index of this set within the exercise (0-based).
     * Used to maintain the sequence of sets.
     */
    val orderIndex: Int
)
