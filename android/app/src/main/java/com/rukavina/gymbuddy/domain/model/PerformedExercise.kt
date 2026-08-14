package com.rukavina.gymbuddy.domain.model

/**
 * Domain model representing a single exercise performed during a workout session.
 * Multiple performed exercises can reference the same Exercise template within one Workout.
 *
 * Each performed exercise contains multiple sets, where each set can have different
 * weight and rep counts.
 *
 * Independent of persistence layer - can be mapped to Room entities or server DTOs.
 *
 * No updatedAt/deletedAt/revision/syncState of its own - this is a child
 * of the WorkoutSession aggregate, and the parent's sync metadata governs
 * the whole tree. A tombstoned WorkoutSession implicitly tombstones every
 * PerformedExercise (and WorkoutSet) under it.
 */
data class PerformedExercise(
    /**
     * Unique identifier for this performed exercise instance.
     */
    val id: String,

    /**
     * Reference to the Exercise template that was performed.
     * Points to Exercise.id, but this is a soft reference, not a foreign
     * key: the exercises table can be re-seeded, the referenced exercise
     * can be deprecated, or (for a custom exercise) deleted entirely.
     * History must remain readable even when this id no longer resolves -
     * use the exerciseName/exerciseCategory/exerciseTrackingType/
     * exercisePrimaryMuscles snapshot below for display, not a live lookup.
     */
    val exerciseId: String,

    /**
     * Order/position of this exercise within the workout session (0-indexed).
     */
    val orderIndex: Int,

    /**
     * Snapshot of Exercise.name at the time this exercise was performed.
     * Written once at creation and never updated - a later rename of a
     * custom exercise, or a library refresh changing the default
     * exercise's name, must not rewrite history.
     */
    val exerciseName: String,

    /**
     * Snapshot of Exercise.category at the time this exercise was performed.
     * Written once at creation and never updated; see exerciseName.
     */
    val exerciseCategory: ExerciseCategory,

    /**
     * Snapshot of Exercise.trackingType at the time this exercise was performed.
     * Written once at creation and never updated; see exerciseName.
     */
    val exerciseTrackingType: ExerciseTrackingType,

    /**
     * Snapshot of Exercise.primaryMuscles at the time this exercise was performed.
     * Written once at creation and never updated; see exerciseName.
     */
    val exercisePrimaryMuscles: List<MuscleGroup>,

    /**
     * List of sets performed for this exercise.
     * Each set can have different weight and reps.
     * Order in the list represents the sequence in which sets were performed.
     */
    val sets: List<WorkoutSet>,

    /**
     * Groups exercises performed together in the same session (e.g. a
     * superset). Exercises sharing a non-null value are performed
     * together. Stored and round-tripped only - no UI or logic yet.
     */
    val supersetGroup: Int? = null
)
