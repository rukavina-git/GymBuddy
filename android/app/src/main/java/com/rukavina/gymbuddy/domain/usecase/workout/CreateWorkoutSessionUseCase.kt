package com.rukavina.gymbuddy.domain.usecase.workout

import com.rukavina.gymbuddy.domain.id.IdGenerator
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.repository.WorkoutSessionRepository
import javax.inject.Inject

/**
 * Use case for creating a new workout session.
 * Handles ID generation and validation logic.
 */
class CreateWorkoutSessionUseCase @Inject constructor(
    private val repository: WorkoutSessionRepository,
    private val idGenerator: IdGenerator,
    private val validateWorkoutSessionSets: ValidateWorkoutSessionSetsUseCase
) {
    /**
     * Create a new workout session with performed exercises.
     * Automatically generates UUIDs for workout session and performed exercises if not provided.
     * @throws IllegalArgumentException if validation fails.
     */
    suspend operator fun invoke(workoutSession: WorkoutSession): Result<Unit> {
        return try {
            // Validate
            require(workoutSession.durationSeconds >= 0) { "Duration must be non-negative" }
            require(workoutSession.date > 0) { "Invalid workout session date" }

            // Generate IDs if empty
            val workoutSessionWithId = if (workoutSession.id.isBlank()) {
                workoutSession.copy(id = idGenerator.newId())
            } else {
                workoutSession
            }

            // Generate IDs for performed exercises if needed
            val performedExercisesWithIds = workoutSessionWithId.performedExercises.map { exercise ->
                if (exercise.id.isBlank()) {
                    exercise.copy(id = idGenerator.newId())
                } else {
                    exercise
                }
            }

            val finalWorkoutSession = workoutSessionWithId.copy(performedExercises = performedExercisesWithIds)

            // Validates sets and stamps each PerformedExercise's exercise
            // snapshot; must use the returned session, not finalWorkoutSession.
            val sessionWithSnapshots = validateWorkoutSessionSets(finalWorkoutSession)

            repository.createWorkoutSession(sessionWithSnapshots)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
