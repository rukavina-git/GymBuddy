package com.rukavina.gymbuddy.data.local.mapper

import com.rukavina.gymbuddy.data.local.entity.ExerciseEntity
import com.rukavina.gymbuddy.data.model.Exercise

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
            difficulty = exercise.difficulty,
            equipmentNeeded = exercise.equipmentNeeded,
            category = exercise.category,
            exerciseType = exercise.exerciseType,
            videoUrl = exercise.videoUrl,
            thumbnailUrl = exercise.thumbnailUrl,
            isCustom = exercise.isCustom,
            createdBy = exercise.createdBy
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
            difficulty = entity.difficulty,
            equipmentNeeded = entity.equipmentNeeded,
            category = entity.category,
            exerciseType = entity.exerciseType,
            videoUrl = entity.videoUrl,
            thumbnailUrl = entity.thumbnailUrl,
            isCustom = entity.isCustom,
            createdBy = entity.createdBy
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
