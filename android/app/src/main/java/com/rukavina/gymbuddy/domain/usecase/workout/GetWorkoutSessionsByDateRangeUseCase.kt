package com.rukavina.gymbuddy.domain.usecase.workout

import com.rukavina.gymbuddy.data.model.WorkoutSession
import com.rukavina.gymbuddy.domain.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving workout sessions within a date range.
 */
class GetWorkoutSessionsByDateRangeUseCase @Inject constructor(
    private val repository: WorkoutSessionRepository
) {
    /**
     * Get workout sessions between start and end dates.
     * @param startDate Unix timestamp in milliseconds
     * @param endDate Unix timestamp in milliseconds
     */
    operator fun invoke(startDate: Long, endDate: Long): Flow<List<WorkoutSession>> {
        return repository.getWorkoutSessionsByDateRange(startDate, endDate)
    }
}
