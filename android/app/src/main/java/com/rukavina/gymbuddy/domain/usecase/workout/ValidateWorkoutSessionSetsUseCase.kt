package com.rukavina.gymbuddy.domain.usecase.workout

import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import com.rukavina.gymbuddy.domain.validation.WorkoutSetValidationResult
import com.rukavina.gymbuddy.domain.validation.WorkoutSetValidator
import javax.inject.Inject

/**
 * Validates every set in a workout session against its exercise's tracking
 * type. Resolves each PerformedExercise's parent Exercise to get its
 * ExerciseTrackingType, then delegates to WorkoutSetValidator.
 *
 * Shared by CreateWorkoutSessionUseCase and UpdateWorkoutSessionUseCase so
 * the check runs on every path that persists a workout session.
 */
class ValidateWorkoutSessionSetsUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) {
    /**
     * @throws IllegalArgumentException with the validator's reason if any
     * completed set is invalid, or if a referenced exercise cannot be found.
     */
    suspend operator fun invoke(workoutSession: WorkoutSession) {
        workoutSession.performedExercises.forEach { performedExercise ->
            val exercise = exerciseRepository.getExerciseById(performedExercise.exerciseId)
                ?: throw IllegalArgumentException("Exercise ${performedExercise.exerciseId} not found")

            performedExercise.sets.forEach { set ->
                when (val result = WorkoutSetValidator.validate(set, exercise.trackingType)) {
                    is WorkoutSetValidationResult.Invalid -> throw IllegalArgumentException(result.reason)
                    WorkoutSetValidationResult.Valid -> Unit
                }
            }
        }
    }
}
