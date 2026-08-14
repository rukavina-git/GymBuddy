package com.rukavina.gymbuddy.data.local.mapper

import com.rukavina.gymbuddy.data.local.entity.TemplateExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutTemplateEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutTemplateWithExercises
import com.rukavina.gymbuddy.domain.model.TemplateExercise
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate

/**
 * Mapper to convert between WorkoutTemplate domain models and Room entities.
 * Handles the relationship between WorkoutTemplate and TemplateExercise.
 *
 * Follows the same pattern as WorkoutSessionMapper for consistency.
 */
object WorkoutTemplateMapper {
    /**
     * Convert domain WorkoutTemplate to Room entities.
     *
     * @param template The domain model to convert
     * @return Pair of WorkoutTemplateEntity and list of TemplateExerciseEntity
     */
    fun toEntities(template: WorkoutTemplate): Pair<WorkoutTemplateEntity, List<TemplateExerciseEntity>> {
        val templateEntity = WorkoutTemplateEntity(
            id = template.id,
            title = template.title,
            source = template.source,
            ownerId = template.ownerId,
            derivedFromId = template.derivedFromId,
            deprecated = template.deprecated,
            updatedAt = template.updatedAt,
            deletedAt = template.deletedAt,
            revision = template.revision,
            syncState = template.syncState
        )

        val exerciseEntities = template.templateExercises.map { exercise ->
            TemplateExerciseEntity(
                id = exercise.id,
                templateId = template.id,
                exerciseId = exercise.exerciseId,
                exerciseName = exercise.exerciseName,
                exerciseTrackingType = exercise.exerciseTrackingType,
                plannedSets = exercise.plannedSets,
                plannedReps = exercise.plannedReps,
                orderIndex = exercise.orderIndex,
                restSeconds = exercise.restSeconds,
                plannedDurationSeconds = exercise.plannedDurationSeconds,
                plannedDistanceMeters = exercise.plannedDistanceMeters,
                plannedWeightKg = exercise.plannedWeightKg,
                notes = exercise.notes
            )
        }

        return templateEntity to exerciseEntities
    }

    /**
     * Convert Room WorkoutTemplateWithExercises to domain WorkoutTemplate.
     * Ensures template exercises are sorted by orderIndex for consistent ordering.
     *
     * @param templateWithExercises The Room relation class to convert
     * @return The domain model with exercises in correct order
     */
    fun toDomain(templateWithExercises: WorkoutTemplateWithExercises): WorkoutTemplate {
        val exercises = templateWithExercises.templateExercises
            .sortedBy { it.orderIndex } // IMPORTANT: Ensure correct exercise order
            .map { entity ->
                TemplateExercise(
                    id = entity.id,
                    exerciseId = entity.exerciseId,
                    exerciseName = entity.exerciseName,
                    exerciseTrackingType = entity.exerciseTrackingType,
                    plannedSets = entity.plannedSets,
                    plannedReps = entity.plannedReps,
                    orderIndex = entity.orderIndex,
                    restSeconds = entity.restSeconds,
                    plannedDurationSeconds = entity.plannedDurationSeconds,
                    plannedDistanceMeters = entity.plannedDistanceMeters,
                    plannedWeightKg = entity.plannedWeightKg,
                    notes = entity.notes
                )
            }

        return WorkoutTemplate(
            id = templateWithExercises.template.id,
            title = templateWithExercises.template.title,
            templateExercises = exercises,
            source = templateWithExercises.template.source,
            ownerId = templateWithExercises.template.ownerId,
            derivedFromId = templateWithExercises.template.derivedFromId,
            deprecated = templateWithExercises.template.deprecated,
            updatedAt = templateWithExercises.template.updatedAt,
            deletedAt = templateWithExercises.template.deletedAt,
            revision = templateWithExercises.template.revision,
            syncState = templateWithExercises.template.syncState
        )
    }

    /**
     * Convert list of WorkoutTemplateWithExercises to list of domain WorkoutTemplates.
     *
     * @param templatesWithExercises List of Room relation classes
     * @return List of domain models
     */
    fun toDomainList(templatesWithExercises: List<WorkoutTemplateWithExercises>): List<WorkoutTemplate> {
        return templatesWithExercises.map { toDomain(it) }
    }

    /**
     * Convert domain TemplateExercise to entity with templateId.
     * Useful for adding a single exercise to an existing template.
     *
     * @param templateExercise The domain model to convert
     * @param templateId The template this exercise belongs to
     * @return The entity ready to be inserted
     */
    fun templateExerciseToEntity(
        templateExercise: TemplateExercise,
        templateId: String
    ): TemplateExerciseEntity {
        return TemplateExerciseEntity(
            id = templateExercise.id,
            templateId = templateId,
            exerciseId = templateExercise.exerciseId,
            exerciseName = templateExercise.exerciseName,
            exerciseTrackingType = templateExercise.exerciseTrackingType,
            plannedSets = templateExercise.plannedSets,
            plannedReps = templateExercise.plannedReps,
            orderIndex = templateExercise.orderIndex,
            restSeconds = templateExercise.restSeconds,
            plannedDurationSeconds = templateExercise.plannedDurationSeconds,
            plannedDistanceMeters = templateExercise.plannedDistanceMeters,
            plannedWeightKg = templateExercise.plannedWeightKg,
            notes = templateExercise.notes
        )
    }
}
