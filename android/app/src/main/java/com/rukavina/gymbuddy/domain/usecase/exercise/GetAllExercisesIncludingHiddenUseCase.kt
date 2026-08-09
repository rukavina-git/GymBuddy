package com.rukavina.gymbuddy.domain.usecase.exercise

import com.rukavina.gymbuddy.data.model.Exercise
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving all exercises including hidden ones.
 * Used for exercise lookups where we need to find exercise names by ID
 * regardless of their hidden status (e.g., when displaying template exercises).
 */
class GetAllExercisesIncludingHiddenUseCase @Inject constructor(
    private val repository: ExerciseRepository
) {
    /**
     * Returns a Flow of all exercises including hidden ones.
     */
    operator fun invoke(): Flow<List<Exercise>> {
        return repository.getAllExercisesIncludingHidden()
    }
}
