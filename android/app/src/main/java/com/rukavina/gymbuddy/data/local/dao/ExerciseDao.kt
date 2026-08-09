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
     * By default, excludes hidden exercises.
     */
    @Query("SELECT * FROM exercises WHERE isHidden = 0 ORDER BY name ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    /**
     * Get all exercises including hidden ones.
     */
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercisesIncludingHidden(): Flow<List<ExerciseEntity>>

    /**
     * Get a single exercise by ID.
     * @return ExerciseEntity if found, null otherwise.
     */
    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Int): ExerciseEntity?

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
    suspend fun deleteExercise(id: Int)

    /**
     * Delete all exercises.
     * Useful for testing or clearing data.
     */
    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()

    /**
     * Get exercises filtered by difficulty level.
     */
    @Query("SELECT * FROM exercises WHERE difficulty = :difficulty ORDER BY name ASC")
    fun getExercisesByDifficulty(difficulty: String): Flow<List<ExerciseEntity>>

    /**
     * Get exercises that require specific equipment.
     * Uses LIKE to match equipment in the comma-separated list.
     */
    @Query("SELECT * FROM exercises WHERE equipmentNeeded LIKE '%' || :equipment || '%' ORDER BY name ASC")
    fun getExercisesByEquipment(equipment: String): Flow<List<ExerciseEntity>>

    /**
     * Get exercises filtered by category.
     */
    @Query("SELECT * FROM exercises WHERE category = :category ORDER BY name ASC")
    fun getExercisesByCategory(category: String): Flow<List<ExerciseEntity>>

    /**
     * Get exercises filtered by type (compound vs isolation).
     */
    @Query("SELECT * FROM exercises WHERE exerciseType = :type ORDER BY name ASC")
    fun getExercisesByType(type: String): Flow<List<ExerciseEntity>>

    /**
     * Get only custom (user-created) exercises.
     */
    @Query("SELECT * FROM exercises WHERE isCustom = 1 ORDER BY name ASC")
    fun getCustomExercises(): Flow<List<ExerciseEntity>>

    /**
     * Get only default (preset) exercises.
     */
    @Query("SELECT * FROM exercises WHERE isCustom = 0 ORDER BY name ASC")
    fun getDefaultExercises(): Flow<List<ExerciseEntity>>

    /**
     * Get exercises that target a specific primary muscle group.
     */
    @Query("SELECT * FROM exercises WHERE primaryMuscles LIKE '%' || :muscleGroup || '%' ORDER BY name ASC")
    fun getExercisesByPrimaryMuscle(muscleGroup: String): Flow<List<ExerciseEntity>>

    /**
     * Get count of all exercises.
     * Useful for checking if database needs seeding.
     */
    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int

    /**
     * Get count of default exercises.
     */
    @Query("SELECT COUNT(*) FROM exercises WHERE isCustom = 0")
    suspend fun getDefaultExerciseCount(): Int

    /**
     * Insert multiple exercises at once.
     * Useful for bulk seeding default exercises.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    /**
     * Delete all default exercises.
     * Useful when updating default exercise library.
     */
    @Query("DELETE FROM exercises WHERE isCustom = 0")
    suspend fun deleteAllDefaultExercises()

    /**
     * Hide an exercise by ID.
     */
    @Query("UPDATE exercises SET isHidden = 1 WHERE id = :id")
    suspend fun hideExercise(id: Int)

    /**
     * Unhide an exercise by ID.
     */
    @Query("UPDATE exercises SET isHidden = 0 WHERE id = :id")
    suspend fun unhideExercise(id: Int)

    /**
     * Get all hidden exercises.
     */
    @Query("SELECT * FROM exercises WHERE isHidden = 1 ORDER BY name ASC")
    fun getHiddenExercises(): Flow<List<ExerciseEntity>>

    /**
     * Unhide all exercises.
     */
    @Query("UPDATE exercises SET isHidden = 0")
    suspend fun unhideAllExercises()
}
