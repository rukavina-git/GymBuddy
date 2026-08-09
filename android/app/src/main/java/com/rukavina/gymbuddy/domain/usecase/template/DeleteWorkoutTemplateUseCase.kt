package com.rukavina.gymbuddy.domain.usecase.template

import com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository
import javax.inject.Inject

/**
 * Use case for deleting a workout template.
 * Also deletes all associated template exercises (cascade).
 */
class DeleteWorkoutTemplateUseCase @Inject constructor(
    private val repository: WorkoutTemplateRepository
) {
    /**
     * Delete a template by ID.
     *
     * @param id The template ID to delete
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(id: String): Result<Unit> {
        return try {
            require(id.isNotBlank()) { "Template ID cannot be blank" }
            repository.deleteTemplate(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
