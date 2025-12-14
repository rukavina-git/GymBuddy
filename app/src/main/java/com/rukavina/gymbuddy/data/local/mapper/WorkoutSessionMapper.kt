package com.rukavina.gymbuddy.data.local.mapper

import com.rukavina.gymbuddy.data.local.entity.PerformedExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutSessionEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutSessionWithPerformedExercises
import com.rukavina.gymbuddy.data.model.PerformedExercise
import com.rukavina.gymbuddy.data.model.WorkoutSession

/**
 * Mapper to convert between WorkoutSession domain models and Room entities.
 * Handles the relationship between WorkoutSession and PerformedExercise.
 */
object WorkoutSessionMapper {
    /**
     * Convert domain WorkoutSession to Room entities.
     * @return Pair of WorkoutSessionEntity and list of PerformedExerciseEntity
     */
    fun toEntities(workoutSession: WorkoutSession): Pair<WorkoutSessionEntity, List<PerformedExerciseEntity>> {
        val workoutSessionEntity = WorkoutSessionEntity(
            id = workoutSession.id,
            date = workoutSession.date,
            durationMinutes = workoutSession.durationMinutes
        )

        val performedExerciseEntities = workoutSession.performedExercises.map { performedExercise ->
            PerformedExerciseEntity(
                id = performedExercise.id,
                workoutSessionId = workoutSession.id,
                exerciseId = performedExercise.exerciseId,
                weight = performedExercise.weight,
                reps = performedExercise.reps,
                sets = performedExercise.sets
            )
        }

        return workoutSessionEntity to performedExerciseEntities
    }

    /**
     * Convert Room WorkoutSessionWithPerformedExercises to domain WorkoutSession.
     */
    fun toDomain(workoutSessionWithExercises: WorkoutSessionWithPerformedExercises): WorkoutSession {
        val performedExercises = workoutSessionWithExercises.performedExercises.map { entity ->
            PerformedExercise(
                id = entity.id,
                exerciseId = entity.exerciseId,
                weight = entity.weight,
                reps = entity.reps,
                sets = entity.sets
            )
        }

        return WorkoutSession(
            id = workoutSessionWithExercises.workoutSession.id,
            date = workoutSessionWithExercises.workoutSession.date,
            durationMinutes = workoutSessionWithExercises.workoutSession.durationMinutes,
            performedExercises = performedExercises
        )
    }

    /**
     * Convert list of WorkoutSessionWithPerformedExercises to list of domain WorkoutSessions.
     */
    fun toDomainList(workoutSessionsWithExercises: List<WorkoutSessionWithPerformedExercises>): List<WorkoutSession> {
        return workoutSessionsWithExercises.map { toDomain(it) }
    }

    /**
     * Convert domain PerformedExercise to entity with workoutSessionId.
     */
    fun performedExerciseToEntity(
        performedExercise: PerformedExercise,
        workoutSessionId: String
    ): PerformedExerciseEntity {
        return PerformedExerciseEntity(
            id = performedExercise.id,
            workoutSessionId = workoutSessionId,
            exerciseId = performedExercise.exerciseId,
            weight = performedExercise.weight,
            reps = performedExercise.reps,
            sets = performedExercise.sets
        )
    }
}
