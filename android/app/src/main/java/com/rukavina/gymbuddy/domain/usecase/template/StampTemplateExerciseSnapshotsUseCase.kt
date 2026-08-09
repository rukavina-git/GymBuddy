package com.rukavina.gymbuddy.domain.usecase.template

import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import javax.inject.Inject

/**
 * Stamps each TemplateExercise's exerciseName and exerciseTrackingType
 * snapshots by resolving its parent Exercise.
 *
 * Shared by CreateWorkoutTemplateUseCase and UpdateWorkoutTemplateUseCase so
 * every path that persists a template stamps snapshots the same way -
 * callers must use the returned WorkoutTemplate, not the one they passed
 * in, since that's the only one with snapshots populated.
 */
class StampTemplateExerciseSnapshotsUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) {
    /**
     * @return the template with every TemplateExercise's exerciseName and
     * exerciseTrackingType populated.
     * @throws IllegalArgumentException if a referenced exercise cannot be found.
     */
    suspend operator fun invoke(template: WorkoutTemplate): WorkoutTemplate {
        val templateExercises = template.templateExercises.map { templateExercise ->
            val exercise = exerciseRepository.getExerciseById(templateExercise.exerciseId)
                ?: throw IllegalArgumentException("Exercise ${templateExercise.exerciseId} not found")

            templateExercise.copy(
                exerciseName = exercise.name,
                exerciseTrackingType = exercise.trackingType
            )
        }

        return template.copy(templateExercises = templateExercises)
    }
}
