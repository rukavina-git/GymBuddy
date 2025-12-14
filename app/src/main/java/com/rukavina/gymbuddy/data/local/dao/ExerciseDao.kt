package com.rukavina.gymbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rukavina.gymbuddy.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Exercise operations.
 * Provides database queries for Exercise table.
 */
@Dao
interface ExerciseDao {
    /**
     * Get all exercises as a Flow for reactive updates.
     * Ordered alphabetically by name.
     */
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    /**
     * Get a single exercise by ID.
     * @return ExerciseEntity if found, null otherwise.
     */
    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: String): ExerciseEntity?

    /**
     * Search exercises by name (case-insensitive).
     * @param query Search query (will be wrapped with % for LIKE query)
     */
    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchExercises(query: String): Flow<List<ExerciseEntity>>

    /**
     * Insert a new exercise.
     * If exercise with same ID exists, replace it.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    /**
     * Update an existing exercise.
     */
    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    /**
     * Delete an exercise by ID.
     */
    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExercise(id: String)

    /**
     * Delete all exercises.
     * Useful for testing or clearing data.
     */
    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()
}
