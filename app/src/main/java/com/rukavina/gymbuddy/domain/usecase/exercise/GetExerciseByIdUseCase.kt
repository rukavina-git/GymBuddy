package com.rukavina.gymbuddy.domain.usecase.exercise

import com.rukavina.gymbuddy.data.model.Exercise
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import javax.inject.Inject

/**
 * Use case for retrieving a single exercise by ID.
 */
class GetExerciseByIdUseCase @Inject constructor(
    private val repository: ExerciseRepository
) {
    /**
     * Get exercise by ID.
     * @return Exercise if found, null otherwise.
     */
    suspend operator fun invoke(id: String): Exercise? {
        return repository.getExerciseById(id)
    }
}
