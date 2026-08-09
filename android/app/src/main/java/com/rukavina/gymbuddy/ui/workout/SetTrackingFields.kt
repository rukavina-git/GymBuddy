package com.rukavina.gymbuddy.ui.workout

import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType

/**
 * Which raw text inputs are shown and/or required for a set of a given
 * exercise tracking type. Mirrors the required-fields table in
 * domain/validation/WorkoutSetValidator.kt so the set entry UI and the
 * save-time validator agree on what a "filled in" set looks like.
 *
 * Shared by ActiveWorkoutViewModel/ActiveWorkoutScreen and WorkoutScreen's
 * manual set entry so both input paths branch the same way.
 */
object SetTrackingFields {
    fun showsWeight(trackingType: ExerciseTrackingType): Boolean = when (trackingType) {
        ExerciseTrackingType.WEIGHT_REPS,
        ExerciseTrackingType.WEIGHT_DURATION,
        ExerciseTrackingType.WEIGHT_DISTANCE -> true
        else -> false
    }

    fun showsReps(trackingType: ExerciseTrackingType): Boolean = when (trackingType) {
        ExerciseTrackingType.WEIGHT_REPS,
        ExerciseTrackingType.REPS_ONLY -> true
        else -> false
    }

    fun showsDuration(trackingType: ExerciseTrackingType): Boolean = when (trackingType) {
        ExerciseTrackingType.WEIGHT_DURATION,
        ExerciseTrackingType.DURATION,
        ExerciseTrackingType.DISTANCE_DURATION -> true
        else -> false
    }

    fun showsDistance(trackingType: ExerciseTrackingType): Boolean = when (trackingType) {
        ExerciseTrackingType.DISTANCE_DURATION,
        ExerciseTrackingType.WEIGHT_DISTANCE -> true
        else -> false
    }

    /**
     * Whether a set's required input(s) for its tracking type have been
     * filled in. Weight is always optional (bodyweight movements), matching
     * WorkoutSetValidator.
     */
    fun isFilled(
        trackingType: ExerciseTrackingType,
        reps: String,
        duration: String,
        distance: String
    ): Boolean = when (trackingType) {
        ExerciseTrackingType.WEIGHT_REPS,
        ExerciseTrackingType.REPS_ONLY -> reps.isNotBlank()

        ExerciseTrackingType.WEIGHT_DURATION,
        ExerciseTrackingType.DURATION -> duration.isNotBlank()

        ExerciseTrackingType.DISTANCE_DURATION -> distance.isNotBlank() && duration.isNotBlank()

        ExerciseTrackingType.WEIGHT_DISTANCE -> distance.isNotBlank()
    }
}
