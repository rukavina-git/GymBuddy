package com.rukavina.gymbuddy.domain.model

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
     * See docs/adr/0001-uuidv7-identifiers.md for the identifier scheme.
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
     * Key tips for performing the exercise correctly.
     * Provides helpful hints and safety advice.
     */
    val tips: List<String> = emptyList(),

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
     * Which measurements a set of this exercise records (weight, reps,
     * duration, distance). Determines which fields the set editor shows.
     */
    val trackingType: ExerciseTrackingType,

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
     * Whether this is a default/bundled (or future server-provided)
     * exercise, or one the user created.
     */
    val source: EntitySource = EntitySource.CUSTOM,

    /**
     * User ID of the creator, for CUSTOM exercises. Null for DEFAULT rows.
     */
    val ownerId: String? = null,

    /**
     * Id of the DEFAULT exercise this one was duplicated from, for a
     * future "duplicate and edit" action on a default entry. Not used yet.
     */
    val derivedFromId: String? = null,

    /**
     * Whether this exercise has been superseded and should no longer
     * appear in lists, search, or filters. Existing references to it
     * remain resolvable. See docs/adr/0002-deprecation-over-deletion.md.
     */
    val deprecated: Boolean = false,

    /**
     * When this row last changed locally, in epoch ms. Set from the
     * injected Clock at write time, not System.currentTimeMillis() -
     * server-assigned once a backend exists.
     *
     * Meaningful for CUSTOM (user-owned) rows; DEFAULT rows are
     * versioned by bulk pull instead, not by this column.
     */
    val updatedAt: Long = 0L,

    /**
     * Tombstone: non-null means this row is deleted. A delete sets this
     * and bumps updatedAt instead of removing the row, so a future sync
     * engine has something to push.
     */
    val deletedAt: Long? = null,

    /**
     * Server-assigned revision number. Stays 0 until a backend exists to
     * assign it - never incremented locally.
     */
    val revision: Int = 0,

    /**
     * Local-only bookkeeping for the future sync engine - see SyncState.
     * Never sent to or received from a server.
     */
    val syncState: SyncState = SyncState.PENDING
)
