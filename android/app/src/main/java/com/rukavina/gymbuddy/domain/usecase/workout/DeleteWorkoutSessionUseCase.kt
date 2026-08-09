package com.rukavina.gymbuddy.domain.usecase.workout

import com.rukavina.gymbuddy.domain.repository.WorkoutSessionRepository
import javax.inject.Inject

/**
 * Use case for deleting a workout session.
 */
class DeleteWorkoutSessionUseCase @Inject constructor(
    private val repository: WorkoutSessionRepository
) {
    /**
     * Delete a workout session by ID.
     */
    suspend operator fun invoke(id: String): Result<Unit> {
        return try {
            require(id.isNotBlank()) { "Workout session ID cannot be blank" }
            repository.deleteWorkoutSession(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
