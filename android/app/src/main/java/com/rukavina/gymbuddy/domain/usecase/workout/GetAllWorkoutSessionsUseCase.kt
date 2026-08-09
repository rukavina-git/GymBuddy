package com.rukavina.gymbuddy.domain.usecase.workout

import com.rukavina.gymbuddy.data.model.WorkoutSession
import com.rukavina.gymbuddy.domain.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving all workout sessions.
 */
class GetAllWorkoutSessionsUseCase @Inject constructor(
    private val repository: WorkoutSessionRepository
) {
    /**
     * Returns a Flow of all workout sessions ordered by date (most recent first).
     */
    operator fun invoke(): Flow<List<WorkoutSession>> {
        return repository.getAllWorkoutSessions()
    }
}
