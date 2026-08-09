package com.rukavina.gymbuddy.domain.model

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
     */
    val id: String,

    /**
     * Reference to the Exercise that this template exercise is based on.
     * Points to Exercise.id (e.g., references "Bench Press", "Squat", etc.).
     */
    val exerciseId: String,

    /**
     * Snapshot of Exercise.name at the time this template exercise was
     * created or last updated. Written once (or re-stamped on update), and
     * never rewritten by an unrelated library refresh or exercise rename.
     */
    val exerciseName: String,

    /**
     * Snapshot of Exercise.trackingType at the time this template exercise
     * was created or last updated. Written once (or re-stamped on update),
     * same rule as exerciseName. Determines which planned measurement
     * (plannedReps, plannedDurationSeconds, plannedDistanceMeters) the
     * display and set editor should use.
     */
    val exerciseTrackingType: ExerciseTrackingType,

    /**
     * Planned/target number of sets for this exercise.
     * Must be greater than 0.
     * Examples: 3, 4, 5
     */
    val plannedSets: Int,

    /**
     * Planned/target number of repetitions per set.
     * Null when the exercise's tracking type has no rep target (e.g. a
     * DURATION exercise like a plank), in which case plannedDurationSeconds
     * carries the target instead.
     * Examples: 8, 10, 12, 15
     */
    val plannedReps: Int? = null,

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
     * Planned/target duration in seconds for this exercise (e.g. a 60s plank).
     * Mirrors WorkoutSet.durationSeconds. Null when not applicable.
     */
    val plannedDurationSeconds: Int? = null,

    /**
     * Planned/target distance in meters for this exercise (e.g. a 2000m row).
     * Mirrors WorkoutSet.distanceMeters. Null when not applicable.
     */
    val plannedDistanceMeters: Float? = null,

    /**
     * Planned/target weight in kilograms for this exercise.
     * Mirrors WorkoutSet.weightKg. Null when not applicable or not planned.
     */
    val plannedWeightKg: Float? = null,

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
