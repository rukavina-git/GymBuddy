package com.rukavina.gymbuddy.data.local.mapper

import com.rukavina.gymbuddy.data.local.entity.TemplateExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutTemplateEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutTemplateWithExercises
import com.rukavina.gymbuddy.data.model.TemplateExercise
import com.rukavina.gymbuddy.data.model.WorkoutTemplate

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
            isDefault = template.isDefault,
            isHidden = template.isHidden
        )

        val exerciseEntities = template.templateExercises.map { exercise ->
            TemplateExerciseEntity(
                id = exercise.id,
                templateId = template.id,
                exerciseId = exercise.exerciseId,
                plannedSets = exercise.plannedSets,
                plannedReps = exercise.plannedReps,
                orderIndex = exercise.orderIndex,
                restSeconds = exercise.restSeconds,
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
                    plannedSets = entity.plannedSets,
                    plannedReps = entity.plannedReps,
                    orderIndex = entity.orderIndex,
                    restSeconds = entity.restSeconds,
                    notes = entity.notes
                )
            }

        return WorkoutTemplate(
            id = templateWithExercises.template.id,
            title = templateWithExercises.template.title,
            templateExercises = exercises,
            isDefault = templateWithExercises.template.isDefault,
            isHidden = templateWithExercises.template.isHidden
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
            plannedSets = templateExercise.plannedSets,
            plannedReps = templateExercise.plannedReps,
            orderIndex = templateExercise.orderIndex,
            restSeconds = templateExercise.restSeconds,
            notes = templateExercise.notes
        )
    }
}
