package com.rukavina.gymbuddy.domain.validation

import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.WorkoutSet

/**
 * Result of validating a WorkoutSet against the measurements its
 * exercise's tracking type requires.
 */
sealed class WorkoutSetValidationResult {
    object Valid : WorkoutSetValidationResult()
    data class Invalid(val reason: String) : WorkoutSetValidationResult()
}

/**
 * Validates a WorkoutSet's measurements against what its exercise's
 * ExerciseTrackingType requires. Pure domain logic - no Android or Room
 * dependencies - so it can run in a fast, plain unit test.
 *
 * An in-progress set (isCompleted = false) always passes: the user may
 * have added a row without typing into it yet. Validation only applies
 * once a set is marked completed.
 */
object WorkoutSetValidator {

    fun validate(set: WorkoutSet, trackingType: ExerciseTrackingType): WorkoutSetValidationResult {
        if (!set.isCompleted) return WorkoutSetValidationResult.Valid

        negativeValueError(set)?.let { return it }

        val error = when (trackingType) {
            ExerciseTrackingType.WEIGHT_REPS, ExerciseTrackingType.REPS_ONLY ->
                repsError(set)

            ExerciseTrackingType.WEIGHT_DURATION, ExerciseTrackingType.DURATION ->
                durationError(set)

            ExerciseTrackingType.WEIGHT_DISTANCE ->
                distanceError(set)

            ExerciseTrackingType.DISTANCE_DURATION ->
                distanceError(set) ?: durationError(set)
        }

        return error ?: WorkoutSetValidationResult.Valid
    }

    private fun negativeValueError(set: WorkoutSet): WorkoutSetValidationResult.Invalid? {
        if ((set.weightKg ?: 0f) < 0f) return invalid("weightKg cannot be negative")
        if ((set.reps ?: 0) < 0) return invalid("reps cannot be negative")
        if ((set.durationSeconds ?: 0) < 0) return invalid("durationSeconds cannot be negative")
        if ((set.distanceMeters ?: 0f) < 0f) return invalid("distanceMeters cannot be negative")
        if ((set.restTakenSeconds ?: 0) < 0) return invalid("restTakenSeconds cannot be negative")
        return null
    }

    private fun repsError(set: WorkoutSet): WorkoutSetValidationResult.Invalid? {
        if (set.reps == null) return invalid("reps is required")
        return null
    }

    private fun durationError(set: WorkoutSet): WorkoutSetValidationResult.Invalid? {
        val duration = set.durationSeconds ?: return invalid("durationSeconds is required")
        if (duration == 0) return invalid("durationSeconds must be greater than zero")
        return null
    }

    private fun distanceError(set: WorkoutSet): WorkoutSetValidationResult.Invalid? {
        val distance = set.distanceMeters ?: return invalid("distanceMeters is required")
        if (distance == 0f) return invalid("distanceMeters must be greater than zero")
        return null
    }

    private fun invalid(reason: String) = WorkoutSetValidationResult.Invalid(reason)
}
