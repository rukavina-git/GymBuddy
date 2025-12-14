package com.rukavina.gymbuddy.domain.usecase.exercise

import com.rukavina.gymbuddy.data.model.Exercise
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving all exercises.
 * Encapsulates business logic and keeps ViewModel simple.
 */
class GetAllExercisesUseCase @Inject constructor(
    private val repository: ExerciseRepository
) {
    /**
     * Returns a Flow of all exercises for reactive UI updates.
     */
    operator fun invoke(): Flow<List<Exercise>> {
        return repository.getAllExercises()
    }
}
