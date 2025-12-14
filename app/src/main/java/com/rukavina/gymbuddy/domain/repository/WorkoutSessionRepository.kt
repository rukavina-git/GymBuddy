package com.rukavina.gymbuddy.domain.repository

import com.rukavina.gymbuddy.data.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for WorkoutSession operations.
 * Abstracts the data source (local Room database, future remote API).
 * Domain layer depends on this interface, not concrete implementations.
 */
interface WorkoutSessionRepository {
    /**
     * Get all workout sessions as a Flow for reactive updates.
     * Ordered by date descending (most recent first).
     */
    fun getAllWorkoutSessions(): Flow<List<WorkoutSession>>

    /**
     * Get a single workout session by ID with all performed exercises.
     * Returns null if not found.
     */
    suspend fun getWorkoutSessionById(id: String): WorkoutSession?

    /**
     * Get workout sessions within a date range.
     * @param startDate Unix timestamp in milliseconds
     * @param endDate Unix timestamp in milliseconds
     */
    fun getWorkoutSessionsByDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutSession>>

    /**
     * Create a new workout session with performed exercises.
     * IDs should be generated (UUID) before calling this.
     */
    suspend fun createWorkoutSession(workoutSession: WorkoutSession)

    /**
     * Update an existing workout session.
     * Replaces the entire workout session including performed exercises.
     */
    suspend fun updateWorkoutSession(workoutSession: WorkoutSession)

    /**
     * Delete a workout session by ID.
     * Also deletes all associated performed exercises (cascade).
     */
    suspend fun deleteWorkoutSession(id: String)
}
