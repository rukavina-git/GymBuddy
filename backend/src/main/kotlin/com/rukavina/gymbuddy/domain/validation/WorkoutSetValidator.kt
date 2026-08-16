package com.rukavina.gymbuddy.domain.validation

import com.rukavina.gymbuddy.api.dto.WorkoutSetSyncDto
import com.rukavina.gymbuddy.domain.ExerciseTrackingType

/**
 * Port of android/app/src/main/java/com/rukavina/gymbuddy/domain/validation/WorkoutSetValidator.kt.
 * Kept a pure function operating on the same field shape so the two
 * implementations stay comparable by eye - this is the authoritative
 * copy; the server never trusts the client's own validation.
 *
 * An in-progress set (isCompleted = false) always passes: the user may
 * have added a row without typing into it yet. Validation only applies
 * once a set is marked completed.
 */
sealed class WorkoutSetValidationResult {
    object Valid : WorkoutSetValidationResult()
    data class Invalid(val reason: String) : WorkoutSetValidationResult()
}

object WorkoutSetValidator {

    fun validate(set: WorkoutSetSyncDto, trackingType: ExerciseTrackingType): WorkoutSetValidationResult {
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

    private fun negativeValueError(set: WorkoutSetSyncDto): WorkoutSetValidationResult.Invalid? {
        if ((set.weightKg ?: 0f) < 0f) return invalid("weightKg cannot be negative")
        if ((set.reps ?: 0) < 0) return invalid("reps cannot be negative")
        if ((set.durationSeconds ?: 0) < 0) return invalid("durationSeconds cannot be negative")
        if ((set.distanceMeters ?: 0f) < 0f) return invalid("distanceMeters cannot be negative")
        if ((set.restTakenSeconds ?: 0) < 0) return invalid("restTakenSeconds cannot be negative")
        return null
    }

    private fun repsError(set: WorkoutSetSyncDto): WorkoutSetValidationResult.Invalid? {
        val reps = set.reps ?: return invalid("reps is required")
        if (reps == 0) return invalid("reps must be greater than zero")
        return null
    }

    private fun durationError(set: WorkoutSetSyncDto): WorkoutSetValidationResult.Invalid? {
        val duration = set.durationSeconds ?: return invalid("durationSeconds is required")
        if (duration == 0) return invalid("durationSeconds must be greater than zero")
        return null
    }

    private fun distanceError(set: WorkoutSetSyncDto): WorkoutSetValidationResult.Invalid? {
        val distance = set.distanceMeters ?: return invalid("distanceMeters is required")
        if (distance == 0f) return invalid("distanceMeters must be greater than zero")
        return null
    }

    private fun invalid(reason: String) = WorkoutSetValidationResult.Invalid(reason)
}
