package com.rukavina.gymbuddy.domain.usecase.workout

import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import com.rukavina.gymbuddy.domain.validation.WorkoutSetValidationResult
import com.rukavina.gymbuddy.domain.validation.WorkoutSetValidator
import javax.inject.Inject

/**
 * Validates every set in a workout session against its exercise's tracking
 * type, and stamps each PerformedExercise's exercise snapshot fields
 * (exerciseName, exerciseCategory, exerciseTrackingType,
 * exercisePrimaryMuscles) from the resolved Exercise.
 *
 * Resolves each PerformedExercise's parent Exercise once and reuses that
 * single lookup for both jobs, rather than querying it twice.
 *
 * Shared by CreateWorkoutSessionUseCase and UpdateWorkoutSessionUseCase so
 * both validation and snapshotting run on every path that persists a
 * workout session - callers must use the returned WorkoutSession, not the
 * one they passed in, since that's the only one with snapshots populated.
 */
class ValidateWorkoutSessionSetsUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) {
    /**
     * @return the workout session with every PerformedExercise's snapshot
     * fields populated.
     * @throws IllegalArgumentException with the validator's reason if any
     * completed set is invalid, or if a referenced exercise cannot be found.
     */
    suspend operator fun invoke(workoutSession: WorkoutSession): WorkoutSession {
        val performedExercises = workoutSession.performedExercises.map { performedExercise ->
            val exercise = exerciseRepository.getExerciseById(performedExercise.exerciseId)
                ?: throw IllegalArgumentException("Exercise ${performedExercise.exerciseId} not found")

            performedExercise.sets.forEach { set ->
                when (val result = WorkoutSetValidator.validate(set, exercise.trackingType)) {
                    is WorkoutSetValidationResult.Invalid -> throw IllegalArgumentException(result.reason)
                    WorkoutSetValidationResult.Valid -> Unit
                }
            }

            performedExercise.copy(
                exerciseName = exercise.name,
                exerciseCategory = exercise.category,
                exerciseTrackingType = exercise.trackingType,
                exercisePrimaryMuscles = exercise.primaryMuscles
            )
        }

        return workoutSession.copy(performedExercises = performedExercises)
    }
}
