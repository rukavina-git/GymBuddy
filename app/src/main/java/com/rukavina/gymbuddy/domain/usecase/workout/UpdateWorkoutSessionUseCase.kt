package com.rukavina.gymbuddy.domain.usecase.workout

import com.rukavina.gymbuddy.data.model.WorkoutSession
import com.rukavina.gymbuddy.domain.repository.WorkoutSessionRepository
import javax.inject.Inject

/**
 * Use case for updating an existing workout session.
 */
class UpdateWorkoutSessionUseCase @Inject constructor(
    private val repository: WorkoutSessionRepository
) {
    /**
     * Update an existing workout session.
     * @throws IllegalArgumentException if validation fails.
     */
    suspend operator fun invoke(workoutSession: WorkoutSession): Result<Unit> {
        return try {
            // Validate
            require(workoutSession.id.isNotBlank()) { "Workout session ID cannot be blank" }
            require(workoutSession.durationSeconds >= 0) { "Duration must be non-negative" }
            require(workoutSession.date > 0) { "Invalid workout session date" }

            repository.updateWorkoutSession(workoutSession)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
