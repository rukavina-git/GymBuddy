package com.rukavina.gymbuddy.data.model

/**
 * Domain model representing a workout template.
 * Templates are blueprints that can be used to quickly start workout sessions.
 * Unlike WorkoutSession, templates only contain planned exercises without actual weight data.
 *
 * Use case: User creates a "Push Day" template with exercises like Bench Press (4x8),
 * Overhead Press (3x10), etc. When starting a workout, they can use this template
 * and fill in the actual weights used during the session.
 *
 * Independent of persistence layer - can be mapped to Room entities or server DTOs.
 */
data class WorkoutTemplate(
    /**
     * Unique identifier for this template.
     * Use UUID string format for offline-first compatibility and server sync.
     */
    val id: String,

    /**
     * User-defined title for the template.
     * Examples: "Push Day", "Pull Day", "Leg Day", "Full Body", "Upper Body Strength"
     */
    val title: String,

    /**
     * List of exercises planned in this template with their target sets/reps.
     * Each exercise includes planning details (sets, reps, rest time) but no weight.
     * Order in the list represents the planned sequence of exercises.
     */
    val templateExercises: List<TemplateExercise>,

    /**
     * Whether this is a default/bundled template.
     * Default templates cannot be edited or deleted, only hidden.
     */
    val isDefault: Boolean = false,

    /**
     * Whether this template is hidden by the user.
     * Hidden templates don't appear in the main list but can be restored.
     */
    val isHidden: Boolean = false
)
