package com.rukavina.gymbuddy.data.model

/**
 * Domain model representing a planned exercise within a workout template.
 * Defines the target sets, reps, order, and optional planning details for an exercise.
 *
 * Key difference from PerformedExercise: This is for PLANNING (no weight data),
 * while PerformedExercise is for EXECUTION (includes actual weight used).
 *
 * Example: In a "Push Day" template, you might have:
 * - Bench Press: 4 sets x 8 reps, 90 seconds rest, note: "Focus on controlled eccentric"
 * - Overhead Press: 3 sets x 10 reps, 60 seconds rest
 *
 * Independent of persistence layer - can be mapped to Room entities or server DTOs.
 */
data class TemplateExercise(
    /**
     * Unique identifier for this template exercise instance.
     * Generated from timestamp for uniqueness.
     */
    val id: Int,

    /**
     * Reference to the Exercise that this template exercise is based on.
     * Points to Exercise.id (e.g., references "Bench Press", "Squat", etc.).
     */
    val exerciseId: Int,

    /**
     * Planned/target number of sets for this exercise.
     * Must be greater than 0.
     * Examples: 3, 4, 5
     */
    val plannedSets: Int,

    /**
     * Planned/target number of repetitions per set.
     * Must be greater than 0.
     * Examples: 8, 10, 12, 15
     */
    val plannedReps: Int,

    /**
     * Order/position of this exercise in the template (0-indexed).
     * Used for maintaining consistent exercise sequence.
     * Examples: 0 (first exercise), 1 (second exercise), etc.
     *
     * This ensures exercises appear in the same order every time:
     * - Compound movements first (e.g., squats, deadlifts)
     * - Isolation movements later (e.g., bicep curls, tricep extensions)
     */
    val orderIndex: Int,

    /**
     * Optional rest time in seconds between sets.
     * Null if no specific rest time is defined.
     *
     * Common values:
     * - Heavy compounds: 180-300 seconds (3-5 minutes)
     * - Regular compounds: 90-120 seconds (1.5-2 minutes)
     * - Isolation: 60-90 seconds (1-1.5 minutes)
     * - High intensity: 30-45 seconds
     */
    val restSeconds: Int? = null,

    /**
     * Optional notes/instructions for this exercise in the template.
     * Can include form cues, intensity guidelines, tempo instructions, etc.
     *
     * Examples:
     * - "Focus on controlled eccentric (3 seconds down)"
     * - "Pause at bottom for 1 second"
     * - "Use tempo 3-0-1-0"
     * - "Go to failure on last set"
     * - "Warm up with 2 sets at 50% weight"
     */
    val notes: String? = null
)
