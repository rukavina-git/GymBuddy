package com.rukavina.gymbuddy.data.local.mapper

import com.rukavina.gymbuddy.data.local.entity.ExerciseEntity
import com.rukavina.gymbuddy.domain.model.Exercise

/**
 * Mapper to convert between Exercise domain model and ExerciseEntity.
 * Keeps domain and data layers decoupled.
 */
object ExerciseMapper {
    /**
     * Convert domain Exercise to Room ExerciseEntity.
     */
    fun toEntity(exercise: Exercise): ExerciseEntity {
        return ExerciseEntity(
            id = exercise.id,
            name = exercise.name,
            primaryMuscles = exercise.primaryMuscles,
            secondaryMuscles = exercise.secondaryMuscles,
            description = exercise.description,
            instructions = exercise.instructions,
            tips = exercise.tips,
            difficulty = exercise.difficulty,
            equipmentNeeded = exercise.equipmentNeeded,
            category = exercise.category,
            exerciseType = exercise.exerciseType,
            trackingType = exercise.trackingType,
            videoUrl = exercise.videoUrl,
            thumbnailUrl = exercise.thumbnailUrl,
            source = exercise.source,
            ownerId = exercise.ownerId,
            derivedFromId = exercise.derivedFromId,
            deprecated = exercise.deprecated
        )
    }

    /**
     * Convert Room ExerciseEntity to domain Exercise.
     */
    fun toDomain(entity: ExerciseEntity): Exercise {
        return Exercise(
            id = entity.id,
            name = entity.name,
            primaryMuscles = entity.primaryMuscles,
            secondaryMuscles = entity.secondaryMuscles,
            description = entity.description,
            instructions = entity.instructions,
            tips = entity.tips,
            difficulty = entity.difficulty,
            equipmentNeeded = entity.equipmentNeeded,
            category = entity.category,
            exerciseType = entity.exerciseType,
            trackingType = entity.trackingType,
            videoUrl = entity.videoUrl,
            thumbnailUrl = entity.thumbnailUrl,
            source = entity.source,
            ownerId = entity.ownerId,
            derivedFromId = entity.derivedFromId,
            deprecated = entity.deprecated
        )
    }

    /**
     * Convert list of entities to list of domain models.
     */
    fun toDomainList(entities: List<ExerciseEntity>): List<Exercise> {
        return entities.map { toDomain(it) }
    }

    /**
     * Convert list of domain models to list of entities.
     */
    fun toEntityList(exercises: List<Exercise>): List<ExerciseEntity> {
        return exercises.map { toEntity(it) }
    }
}
