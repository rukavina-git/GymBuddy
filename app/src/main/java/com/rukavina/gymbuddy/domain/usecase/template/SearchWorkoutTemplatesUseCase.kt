package com.rukavina.gymbuddy.domain.usecase.template

import com.rukavina.gymbuddy.data.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for searching workout templates by title.
 * Performs case-insensitive search.
 */
class SearchWorkoutTemplatesUseCase @Inject constructor(
    private val repository: WorkoutTemplateRepository
) {
    /**
     * Search templates by query string.
     *
     * @param query Search query (case-insensitive)
     * @return Flow of matching templates
     */
    operator fun invoke(query: String): Flow<List<WorkoutTemplate>> {
        return repository.searchTemplates(query)
    }
}
