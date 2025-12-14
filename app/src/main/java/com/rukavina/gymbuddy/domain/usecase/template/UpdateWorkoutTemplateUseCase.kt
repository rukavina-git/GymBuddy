package com.rukavina.gymbuddy.domain.usecase.template

import com.rukavina.gymbuddy.data.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository
import javax.inject.Inject

/**
 * Use case for updating an existing workout template.
 */
class UpdateWorkoutTemplateUseCase @Inject constructor(
    private val repository: WorkoutTemplateRepository
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
                require(exercise.id.isNotBlank()) { "Exercise ID cannot be blank" }
                require(exercise.exerciseId.isNotBlank()) { "Exercise reference ID cannot be blank" }
                require(exercise.plannedSets > 0) { "Planned sets must be greater than 0" }
                require(exercise.plannedReps > 0) { "Planned reps must be greater than 0" }
                require(exercise.orderIndex >= 0) { "Order index must be non-negative" }
                exercise.restSeconds?.let { rest ->
                    require(rest > 0) { "Rest seconds must be greater than 0 if specified" }
                }
            }

            repository.updateTemplate(template)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
