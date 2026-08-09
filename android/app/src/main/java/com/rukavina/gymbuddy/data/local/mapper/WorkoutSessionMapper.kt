package com.rukavina.gymbuddy.data.local.mapper

import com.rukavina.gymbuddy.data.local.entity.PerformedExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.PerformedExerciseWithSets
import com.rukavina.gymbuddy.data.local.entity.WorkoutSessionEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutSessionWithPerformedExercises
import com.rukavina.gymbuddy.data.local.entity.WorkoutSetEntity
import com.rukavina.gymbuddy.data.model.PerformedExercise
import com.rukavina.gymbuddy.data.model.WorkoutSession
import com.rukavina.gymbuddy.data.model.WorkoutSet

/**
 * Mapper to convert between WorkoutSession domain models and Room entities.
 * Handles the relationship between WorkoutSession, PerformedExercise, and WorkoutSet.
 */
object WorkoutSessionMapper {
    /**
     * Convert domain WorkoutSession to Room entities.
     * @return Triple of WorkoutSessionEntity, list of PerformedExerciseEntity, and list of WorkoutSetEntity
     */
    fun toEntities(workoutSession: WorkoutSession): Triple<WorkoutSessionEntity, List<PerformedExerciseEntity>, List<WorkoutSetEntity>> {
        val workoutSessionEntity = WorkoutSessionEntity(
            id = workoutSession.id,
            date = workoutSession.date,
            durationSeconds = workoutSession.durationSeconds,
            title = workoutSession.title
        )

        val performedExerciseEntities = mutableListOf<PerformedExerciseEntity>()
        val workoutSetEntities = mutableListOf<WorkoutSetEntity>()

        workoutSession.performedExercises.forEach { performedExercise ->
            performedExerciseEntities.add(
                PerformedExerciseEntity(
                    id = performedExercise.id,
                    workoutSessionId = workoutSession.id,
                    exerciseId = performedExercise.exerciseId
                )
            )

            performedExercise.sets.forEach { set ->
                workoutSetEntities.add(
                    WorkoutSetEntity(
                        id = set.id,
                        performedExerciseId = performedExercise.id,
                        weight = set.weight,
                        reps = set.reps,
                        orderIndex = set.orderIndex
                    )
                )
            }
        }

        return Triple(workoutSessionEntity, performedExerciseEntities, workoutSetEntities)
    }

    /**
     * Convert Room WorkoutSessionWithPerformedExercises to domain WorkoutSession.
     * Requires performed exercises with their sets to be fetched separately.
     */
    fun toDomain(
        workoutSessionEntity: WorkoutSessionEntity,
        performedExercisesWithSets: List<PerformedExerciseWithSets>
    ): WorkoutSession {
        val performedExercises = performedExercisesWithSets.map { performedExerciseWithSets ->
            val sets = performedExerciseWithSets.sets
                .sortedBy { it.orderIndex }
                .map { setEntity ->
                    WorkoutSet(
                        id = setEntity.id,
                        weight = setEntity.weight,
                        reps = setEntity.reps,
                        orderIndex = setEntity.orderIndex
                    )
                }

            PerformedExercise(
                id = performedExerciseWithSets.performedExercise.id,
                exerciseId = performedExerciseWithSets.performedExercise.exerciseId,
                sets = sets
            )
        }

        return WorkoutSession(
            id = workoutSessionEntity.id,
            date = workoutSessionEntity.date,
            durationSeconds = workoutSessionEntity.durationSeconds,
            title = workoutSessionEntity.title,
            performedExercises = performedExercises
        )
    }

    /**
     * Convert domain PerformedExercise to entities with workoutSessionId.
     * @return Pair of PerformedExerciseEntity and list of WorkoutSetEntity
     */
    fun performedExerciseToEntities(
        performedExercise: PerformedExercise,
        workoutSessionId: String
    ): Pair<PerformedExerciseEntity, List<WorkoutSetEntity>> {
        val performedExerciseEntity = PerformedExerciseEntity(
            id = performedExercise.id,
            workoutSessionId = workoutSessionId,
            exerciseId = performedExercise.exerciseId
        )

        val workoutSetEntities = performedExercise.sets.map { set ->
            WorkoutSetEntity(
                id = set.id,
                performedExerciseId = performedExercise.id,
                weight = set.weight,
                reps = set.reps,
                orderIndex = set.orderIndex
            )
        }

        return performedExerciseEntity to workoutSetEntities
    }
}
