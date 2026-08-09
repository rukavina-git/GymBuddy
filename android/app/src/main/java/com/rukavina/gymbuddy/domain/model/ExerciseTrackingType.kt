package com.rukavina.gymbuddy.domain.model

/**
 * Determines which measurements a set of this exercise records.
 * WEIGHT_REPS is the default and covers the large majority of exercises;
 * the others exist for bodyweight, timed, and cardio-style movements.
 */
enum class ExerciseTrackingType {
    WEIGHT_REPS,        // weight + reps; weight optional
    REPS_ONLY,          // reps only
    WEIGHT_DURATION,    // duration + optional weight
    DURATION,           // duration only
    DISTANCE_DURATION,  // distance + duration
    WEIGHT_DISTANCE     // distance + optional weight
}
