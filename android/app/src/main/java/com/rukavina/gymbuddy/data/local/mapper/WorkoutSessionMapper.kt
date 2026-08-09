package com.rukavina.gymbuddy.data.local.mapper

import com.rukavina.gymbuddy.data.local.entity.PerformedExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.PerformedExerciseWithSets
import com.rukavina.gymbuddy.data.local.entity.WorkoutSessionEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutSetEntity
import com.rukavina.gymbuddy.domain.model.PerformedExercise
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutSet

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
            startedAt = workoutSession.startedAt,
            endedAt = workoutSession.endedAt,
            durationSeconds = workoutSession.durationSeconds,
            title = workoutSession.title,
            notes = workoutSession.notes,
            templateId = workoutSession.templateId,
            templateTitle = workoutSession.templateTitle
        )

        val performedExerciseEntities = mutableListOf<PerformedExerciseEntity>()
        val workoutSetEntities = mutableListOf<WorkoutSetEntity>()

        workoutSession.performedExercises.forEach { performedExercise ->
            performedExerciseEntities.add(
                PerformedExerciseEntity(
                    id = performedExercise.id,
                    workoutSessionId = workoutSession.id,
                    exerciseId = performedExercise.exerciseId,
                    orderIndex = performedExercise.orderIndex,
                    exerciseName = performedExercise.exerciseName,
                    exerciseCategory = performedExercise.exerciseCategory,
                    exerciseTrackingType = performedExercise.exerciseTrackingType,
                    exercisePrimaryMuscles = performedExercise.exercisePrimaryMuscles,
                    supersetGroup = performedExercise.supersetGroup
                )
            )

            performedExercise.sets.forEach { set ->
                workoutSetEntities.add(
                    WorkoutSetEntity(
                        id = set.id,
                        performedExerciseId = performedExercise.id,
                        weightKg = set.weightKg,
                        reps = set.reps,
                        durationSeconds = set.durationSeconds,
                        distanceMeters = set.distanceMeters,
                        setType = set.setType,
                        isCompleted = set.isCompleted,
                        restTakenSeconds = set.restTakenSeconds,
                        orderIndex = set.orderIndex
                    )
                )
            }
        }

        return Triple(workoutSessionEntity, performedExerciseEntities, workoutSetEntities)
    }

    /**
     * Convert a Room WorkoutSessionEntity plus its performed exercises (with
     * sets) to a domain WorkoutSession. Both performed exercises and sets are
     * expected to already be ordered by orderIndex - see
     * WorkoutSessionDao.getPerformedExercisesWithSets, which does that in SQL.
     */
    fun toDomain(
        workoutSessionEntity: WorkoutSessionEntity,
        performedExercisesWithSets: List<PerformedExerciseWithSets>
    ): WorkoutSession {
        val performedExercises = performedExercisesWithSets.map { performedExerciseWithSets ->
            val sets = performedExerciseWithSets.sets.map { setEntity ->
                WorkoutSet(
                    id = setEntity.id,
                    weightKg = setEntity.weightKg,
                    reps = setEntity.reps,
                    durationSeconds = setEntity.durationSeconds,
                    distanceMeters = setEntity.distanceMeters,
                    setType = setEntity.setType,
                    isCompleted = setEntity.isCompleted,
                    restTakenSeconds = setEntity.restTakenSeconds,
                    orderIndex = setEntity.orderIndex
                )
            }

            val entity = performedExerciseWithSets.performedExercise
            PerformedExercise(
                id = entity.id,
                exerciseId = entity.exerciseId,
                orderIndex = entity.orderIndex,
                exerciseName = entity.exerciseName,
                exerciseCategory = entity.exerciseCategory,
                exerciseTrackingType = entity.exerciseTrackingType,
                exercisePrimaryMuscles = entity.exercisePrimaryMuscles,
                sets = sets,
                supersetGroup = entity.supersetGroup
            )
        }

        return WorkoutSession(
            id = workoutSessionEntity.id,
            startedAt = workoutSessionEntity.startedAt,
            endedAt = workoutSessionEntity.endedAt,
            durationSeconds = workoutSessionEntity.durationSeconds,
            title = workoutSessionEntity.title,
            notes = workoutSessionEntity.notes,
            templateId = workoutSessionEntity.templateId,
            templateTitle = workoutSessionEntity.templateTitle,
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
            exerciseId = performedExercise.exerciseId,
            orderIndex = performedExercise.orderIndex,
            exerciseName = performedExercise.exerciseName,
            exerciseCategory = performedExercise.exerciseCategory,
            exerciseTrackingType = performedExercise.exerciseTrackingType,
            exercisePrimaryMuscles = performedExercise.exercisePrimaryMuscles,
            supersetGroup = performedExercise.supersetGroup
        )

        val workoutSetEntities = performedExercise.sets.map { set ->
            WorkoutSetEntity(
                id = set.id,
                performedExerciseId = performedExercise.id,
                weightKg = set.weightKg,
                reps = set.reps,
                durationSeconds = set.durationSeconds,
                distanceMeters = set.distanceMeters,
                setType = set.setType,
                isCompleted = set.isCompleted,
                restTakenSeconds = set.restTakenSeconds,
                orderIndex = set.orderIndex
            )
        }

        return performedExerciseEntity to workoutSetEntities
    }
}
