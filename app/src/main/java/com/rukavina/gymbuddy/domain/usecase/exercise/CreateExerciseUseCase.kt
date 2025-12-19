package com.rukavina.gymbuddy.domain.usecase.exercise

import com.rukavina.gymbuddy.data.model.Exercise
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import javax.inject.Inject

/**
 * Use case for creating a new exercise.
 * Handles ID generation and validation logic.
 * Custom exercises get IDs from current timestamp (6+ digits).
 */
class CreateExerciseUseCase @Inject constructor(
    private val repository: ExerciseRepository
) {
    /**
     * Create a new exercise.
     * Automatically generates ID from timestamp if not provided or 0.
     * @throws IllegalArgumentException if exercise name is blank.
     */
    suspend operator fun invoke(exercise: Exercise): Result<Unit> {
        return try {
            // Validate
            require(exercise.name.isNotBlank()) { "Exercise name cannot be blank" }
            require(exercise.primaryMuscles.isNotEmpty()) { "Exercise must target at least one primary muscle" }

            // Generate ID if not set (0 or negative)
            val exerciseWithId = if (exercise.id <= 0) {
                // Use timestamp to generate unique ID (will be 10+ digits)
                exercise.copy(id = System.currentTimeMillis().toInt())
            } else {
                exercise
            }

            repository.createExercise(exerciseWithId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
