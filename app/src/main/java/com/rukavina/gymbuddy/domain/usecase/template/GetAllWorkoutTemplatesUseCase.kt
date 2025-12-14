package com.rukavina.gymbuddy.domain.usecase.template

import com.rukavina.gymbuddy.data.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving all workout templates.
 * Returns templates ordered alphabetically by title.
 */
class GetAllWorkoutTemplatesUseCase @Inject constructor(
    private val repository: WorkoutTemplateRepository
) {
    /**
     * Returns a Flow of all templates.
     * UI will automatically update when templates change.
     */
    operator fun invoke(): Flow<List<WorkoutTemplate>> {
        return repository.getAllTemplates()
    }
}
