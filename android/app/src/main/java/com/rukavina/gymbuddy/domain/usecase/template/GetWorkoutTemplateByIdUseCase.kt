package com.rukavina.gymbuddy.domain.usecase.template

import com.rukavina.gymbuddy.data.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository
import javax.inject.Inject

/**
 * Use case for retrieving a single workout template by ID.
 */
class GetWorkoutTemplateByIdUseCase @Inject constructor(
    private val repository: WorkoutTemplateRepository
) {
    /**
     * Get template by ID.
     * Returns null if template not found.
     *
     * @param id The template ID
     * @return The template with exercises, or null
     */
    suspend operator fun invoke(id: String): WorkoutTemplate? {
        return repository.getTemplateById(id)
    }
}
