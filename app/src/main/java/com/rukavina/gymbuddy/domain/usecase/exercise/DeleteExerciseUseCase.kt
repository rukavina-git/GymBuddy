package com.rukavina.gymbuddy.domain.usecase.exercise

import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import javax.inject.Inject

/**
 * Use case for deleting an exercise.
 */
class DeleteExerciseUseCase @Inject constructor(
    private val repository: ExerciseRepository
) {
    /**
     * Delete an exercise by ID.
     */
    suspend operator fun invoke(id: String): Result<Unit> {
        return try {
            require(id.isNotBlank()) { "Exercise ID cannot be blank" }
            repository.deleteExercise(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
