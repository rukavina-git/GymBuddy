package com.rukavina.gymbuddy.domain.usecase.exercise

import com.rukavina.gymbuddy.data.model.Exercise
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Use case for creating a new exercise.
 * Handles ID generation and validation logic.
 */
class CreateExerciseUseCase @Inject constructor(
    private val repository: ExerciseRepository
) {
    /**
     * Create a new exercise.
     * Automatically generates UUID if not provided.
     * @throws IllegalArgumentException if exercise name is blank.
     */
    suspend operator fun invoke(exercise: Exercise): Result<Unit> {
        return try {
            // Validate
            require(exercise.name.isNotBlank()) { "Exercise name cannot be blank" }
            require(exercise.primaryMuscles.isNotEmpty()) { "Exercise must target at least one primary muscle" }

            // Generate ID if empty
            val exerciseWithId = if (exercise.id.isBlank()) {
                exercise.copy(id = UUID.randomUUID().toString())
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
