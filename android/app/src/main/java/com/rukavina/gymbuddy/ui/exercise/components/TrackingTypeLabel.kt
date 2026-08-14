package com.rukavina.gymbuddy.ui.exercise.components

import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType

/**
 * Plain-language label for an ExerciseTrackingType, for use anywhere the
 * picker shows the choice to a user instead of the enum name.
 */
fun ExerciseTrackingType.displayLabel(): String = when (this) {
    ExerciseTrackingType.WEIGHT_REPS -> "Weight and reps"
    ExerciseTrackingType.REPS_ONLY -> "Reps only"
    ExerciseTrackingType.DURATION -> "Time"
    ExerciseTrackingType.WEIGHT_DURATION -> "Weight and time"
    ExerciseTrackingType.DISTANCE_DURATION -> "Distance and time"
    ExerciseTrackingType.WEIGHT_DISTANCE -> "Weight and distance"
}
