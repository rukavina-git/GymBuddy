package com.rukavina.gymbuddy.domain.repository

import com.rukavina.gymbuddy.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Exercise operations.
 * Abstracts the data source (local Room database, future remote API).
 * Domain layer depends on this interface, not concrete implementations.
 */
interface ExerciseRepository {
    /**
     * Get all exercises as a Flow for reactive updates.
     * UI will automatically update when data changes.
     */
    fun getAllExercises(): Flow<List<Exercise>>

    /**
     * Get a single exercise by ID.
     * Returns null if not found.
     */
    suspend fun getExerciseById(id: String): Exercise?

    /**
     * Create a new exercise.
     * ID should be generated before calling this.
     */
    suspend fun createExercise(exercise: Exercise)

    /**
     * Update an existing exercise.
     * Replaces the entire exercise with the new data.
     */
    suspend fun updateExercise(exercise: Exercise)

    /**
     * Delete an exercise by ID.
     */
    suspend fun deleteExercise(id: String)

    /**
     * Hide an exercise (only for default exercises).
     */
    suspend fun hideExercise(id: String)

    /**
     * Unhide an exercise.
     */
    suspend fun unhideExercise(id: String)

    /**
     * Get all hidden exercises.
     */
    fun getHiddenExercises(): Flow<List<Exercise>>

    /**
     * Unhide all exercises at once.
     */
    suspend fun unhideAllExercises()

    /**
     * Get the personal note for an exercise, or null if none has been set.
     */
    suspend fun getExerciseNote(exerciseId: String): String?

    /**
     * Set (or clear, with null) the personal note for an exercise.
     */
    suspend fun updateExerciseNote(exerciseId: String, note: String?)

    /**
     * Search exercises by name (case-insensitive).
     * Useful for autocomplete or filtering.
     */
    fun searchExercises(query: String): Flow<List<Exercise>>

    /**
     * Get all exercises including hidden ones.
     * Useful for lookups where we need to find exercise names by ID
     * regardless of hidden status.
     */
    fun getAllExercisesIncludingHidden(): Flow<List<Exercise>>
}
