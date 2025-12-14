package com.rukavina.gymbuddy.domain.usecase.exercise

import com.rukavina.gymbuddy.data.model.Exercise
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import javax.inject.Inject

/**
 * Use case for updating an existing exercise.
 */
class UpdateExerciseUseCase @Inject constructor(
    private val repository: ExerciseRepository
) {
    /**
     * Update an exercise.
     * @throws IllegalArgumentException if validation fails.
     */
    suspend operator fun invoke(exercise: Exercise): Result<Unit> {
        return try {
            require(exercise.id.isNotBlank()) { "Exercise ID cannot be blank" }
            require(exercise.name.isNotBlank()) { "Exercise name cannot be blank" }
            require(exercise.primaryMuscles.isNotEmpty()) { "Exercise must target at least one primary muscle" }

            repository.updateExercise(exercise)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
