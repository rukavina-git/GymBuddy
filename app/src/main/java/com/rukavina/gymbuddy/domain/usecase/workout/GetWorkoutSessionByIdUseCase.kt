package com.rukavina.gymbuddy.domain.usecase.workout

import com.rukavina.gymbuddy.data.model.WorkoutSession
import com.rukavina.gymbuddy.domain.repository.WorkoutSessionRepository
import javax.inject.Inject

/**
 * Use case for retrieving a single workout session by ID.
 */
class GetWorkoutSessionByIdUseCase @Inject constructor(
    private val repository: WorkoutSessionRepository
) {
    /**
     * Get workout session by ID. Returns null if not found.
     */
    suspend operator fun invoke(id: String): WorkoutSession? {
        return repository.getWorkoutSessionById(id)
    }
}
