package com.rukavina.gymbuddy.domain.usecase.exercise

import com.rukavina.gymbuddy.data.model.Exercise
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for searching exercises by name.
 */
class SearchExercisesUseCase @Inject constructor(
    private val repository: ExerciseRepository
) {
    /**
     * Search exercises by query string.
     * Returns reactive Flow of matching exercises.
     */
    operator fun invoke(query: String): Flow<List<Exercise>> {
        return repository.searchExercises(query)
    }
}
