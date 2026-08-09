package com.rukavina.gymbuddy.domain.usecase.template

import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository
import javax.inject.Inject

/**
 * Use case for updating an existing workout template.
 */
class UpdateWorkoutTemplateUseCase @Inject constructor(
    private val repository: WorkoutTemplateRepository,
    private val stampTemplateExerciseSnapshots: StampTemplateExerciseSnapshotsUseCase
) {
    /**
     * Update an existing template.
     * Validates template data before updating.
     *
     * @param template The updated template
     * @return Result indicating success or failure with error message
     * @throws IllegalArgumentException if validation fails
     */
    suspend operator fun invoke(template: WorkoutTemplate): Result<Unit> {
        return try {
            // Validate template
            require(template.id.isNotBlank()) { "Template ID cannot be blank" }
            require(template.title.isNotBlank()) { "Template title cannot be blank" }
            require(template.templateExercises.isNotEmpty()) { "Template must have at least one exercise" }

            // Validate each exercise
            template.templateExercises.forEach { exercise ->
                require(exercise.id.isNotBlank()) { "Exercise ID must be valid" }
                require(exercise.exerciseId.isNotBlank()) { "Exercise reference ID must be valid" }
                require(exercise.plannedSets > 0) { "Planned sets must be greater than 0" }
                exercise.plannedReps?.let { reps ->
                    require(reps > 0) { "Planned reps must be greater than 0 if specified" }
                }
                require(exercise.orderIndex >= 0) { "Order index must be non-negative" }
                exercise.restSeconds?.let { rest ->
                    require(rest > 0) { "Rest seconds must be greater than 0 if specified" }
                }
            }

            // Stamps each TemplateExercise's exerciseName; must use the
            // returned template, not the original.
            val templateWithSnapshots = stampTemplateExerciseSnapshots(template)

            repository.updateTemplate(templateWithSnapshots)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
