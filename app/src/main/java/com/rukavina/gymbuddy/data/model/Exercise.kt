package com.rukavina.gymbuddy.data.model

/**
 * Domain model representing an exercise template/definition.
 * This is not a performed instance, but rather the blueprint of an exercise type
 * (e.g., "Bench Press", "Squat", "Deadlift").
 *
 * Independent of persistence layer - can be mapped to Room entities or server DTOs.
 */
data class Exercise(
    /**
     * Unique identifier for the exercise.
     * Use UUID string format for offline-first compatibility and server sync.
     * Generated locally when created offline, persisted when synced to server.
     */
    val id: String,

    /**
     * Name of the exercise (e.g., "Bench Press", "Barbell Squat").
     */
    val name: String,

    /**
     * Primary muscle groups targeted by this exercise.
     * An exercise can target multiple primary muscles (e.g., compound movements).
     */
    val primaryMuscles: List<MuscleGroup>,

    /**
     * Secondary muscle groups engaged during this exercise.
     * These are muscles that assist but are not the main focus.
     */
    val secondaryMuscles: List<MuscleGroup>
)
