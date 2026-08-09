package com.rukavina.gymbuddy.domain.usecase.template

import com.rukavina.gymbuddy.domain.id.IdGenerator
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository
import javax.inject.Inject

/**
 * Use case for creating a new workout template.
 * Handles ID generation and validation logic.
 */
class CreateWorkoutTemplateUseCase @Inject constructor(
    private val repository: WorkoutTemplateRepository,
    private val idGenerator: IdGenerator,
    private val stampTemplateExerciseSnapshots: StampTemplateExerciseSnapshotsUseCase
) {
    /**
     * Create a new template with exercises.
     * Automatically generates UUIDs for template and exercises if not provided.
     *
     * @param template The template to create
     * @return Result indicating success or failure with error message
     * @throws IllegalArgumentException if validation fails
     */
    suspend operator fun invoke(template: WorkoutTemplate): Result<Unit> {
        return try {
            // Validate template
            require(template.title.isNotBlank()) { "Template title cannot be blank" }
            require(template.templateExercises.isNotEmpty()) { "Template must have at least one exercise" }

            // Validate each exercise
            template.templateExercises.forEach { exercise ->
                require(exercise.exerciseId.isNotBlank()) { "Exercise ID must be valid" }
                require(exercise.plannedSets > 0) { "Planned sets must be greater than 0" }
                exercise.plannedReps?.let { reps ->
                    require(reps > 0) { "Planned reps must be greater than 0 if specified" }
                }
                require(exercise.orderIndex >= 0) { "Order index must be non-negative" }
                exercise.restSeconds?.let { rest ->
                    require(rest > 0) { "Rest seconds must be greater than 0 if specified" }
                }
            }

            // Generate ID if empty
            val templateWithId = if (template.id.isBlank()) {
                template.copy(id = idGenerator.newId())
            } else {
                template
            }

            // Generate IDs for exercises if needed
            val exercisesWithIds = templateWithId.templateExercises.map { exercise ->
                if (exercise.id.isBlank()) {
                    exercise.copy(id = idGenerator.newId())
                } else {
                    exercise
                }
            }

            val finalTemplate = templateWithId.copy(templateExercises = exercisesWithIds)

            // Stamps each TemplateExercise's exerciseName; must use the
            // returned template, not finalTemplate.
            val templateWithSnapshots = stampTemplateExerciseSnapshots(finalTemplate)

            repository.createTemplate(templateWithSnapshots)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
