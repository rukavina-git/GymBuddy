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
    val secondaryMuscles: List<MuscleGroup>,

    /**
     * Detailed description of how to perform the exercise.
     * Includes form cues, breathing tips, and general guidance.
     */
    val description: String? = null,

    /**
     * Step-by-step instructions for performing the exercise.
     * Each string represents one step in the execution.
     */
    val instructions: List<String> = emptyList(),

    /**
     * Difficulty level of the exercise.
     * Helps users select appropriate exercises for their skill level.
     */
    val difficulty: DifficultyLevel,

    /**
     * Equipment required to perform this exercise.
     * Can include multiple items (e.g., barbell + bench).
     */
    val equipmentNeeded: List<Equipment>,

    /**
     * General category of the exercise.
     * Classifies by training modality (strength, cardio, etc.).
     */
    val category: ExerciseCategory,

    /**
     * Type of exercise based on joint involvement.
     * Compound exercises use multiple joints, isolation exercises use one.
     */
    val exerciseType: ExerciseType,

    /**
     * URL to an external video demonstration (e.g., YouTube).
     * Provides visual guidance for proper form.
     */
    val videoUrl: String? = null,

    /**
     * URL to a thumbnail image of the exercise.
     * Used for preview in lists and cards.
     */
    val thumbnailUrl: String? = null,

    /**
     * Indicates whether this is a user-created custom exercise.
     * false = default/preset exercise shipped with the app or from backend
     * true = created by the user
     */
    val isCustom: Boolean = true,

    /**
     * User ID of the creator (for custom exercises).
     * null for default exercises.
     */
    val createdBy: String? = null
)
