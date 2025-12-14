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
            secondaryMuscles = exercise.secondaryMuscles
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
            secondaryMuscles = entity.secondaryMuscles
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
