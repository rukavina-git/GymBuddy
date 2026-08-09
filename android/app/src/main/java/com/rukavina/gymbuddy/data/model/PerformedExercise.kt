package com.rukavina.gymbuddy.data.model

/**
 * Domain model representing a single exercise performed during a workout session.
 * Multiple performed exercises can reference the same Exercise template within one Workout.
 *
 * Each performed exercise contains multiple sets, where each set can have different
 * weight and rep counts.
 *
 * Independent of persistence layer - can be mapped to Room entities or server DTOs.
 */
data class PerformedExercise(
    /**
     * Unique identifier for this performed exercise instance.
     * Generated from timestamp for uniqueness.
     */
    val id: Int,

    /**
     * Reference to the Exercise template that was performed.
     * Points to Exercise.id.
     */
    val exerciseId: Int,

    /**
     * List of sets performed for this exercise.
     * Each set can have different weight and reps.
     * Order in the list represents the sequence in which sets were performed.
     */
    val sets: List<WorkoutSet>
)
