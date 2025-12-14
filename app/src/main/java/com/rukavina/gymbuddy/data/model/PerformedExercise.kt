package com.rukavina.gymbuddy.data.model

/**
 * Domain model representing a single exercise performed during a workout session.
 * Multiple performed exercises can reference the same Exercise template within one Workout.
 *
 * For example, if a workout includes 3 sets of bench press followed by 3 sets of incline bench press,
 * there would be two PerformedExercise instances, both potentially referencing different Exercise templates.
 *
 * Independent of persistence layer - can be mapped to Room entities or server DTOs.
 */
data class PerformedExercise(
    /**
     * Unique identifier for this performed exercise instance.
     * Use UUID string format for offline-first compatibility and server sync.
     */
    val id: String,

    /**
     * Reference to the Exercise template that was performed.
     * Points to Exercise.id.
     */
    val exerciseId: String,

    /**
     * Weight used for this exercise in the user's preferred unit (kg or lbs).
     * Use Float for sufficient precision while maintaining reasonable storage size.
     */
    val weight: Float,

    /**
     * Number of repetitions performed per set.
     */
    val reps: Int,

    /**
     * Number of sets completed for this exercise.
     */
    val sets: Int
)
