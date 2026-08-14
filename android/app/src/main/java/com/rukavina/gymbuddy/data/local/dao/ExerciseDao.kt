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
 *
 * Hidden/favorite/note state lives in user_exercise_state, not on the
 * exercises row itself - see UserExerciseStateEntity. List/search/filter
 * queries below LEFT JOIN that table only to exclude hidden exercises;
 * they never need to project its columns since Exercise no longer carries
 * isHidden/note.
 *
 * Every read query also excludes deletedAt IS NOT NULL rows (tombstoned
 * CUSTOM exercises) - including detail lookups by id: unlike hidden or
 * deprecated, a deleted row is gone from this device's perspective, and
 * history that referenced it already has its own name/category/etc.
 * snapshot and never looks it up live.
 */
@Dao
interface ExerciseDao {
    /**
     * Get all exercises as a Flow for reactive updates.
     * Ordered alphabetically by name.
     * Excludes hidden, deprecated and deleted exercises.
     */
    @Query(
        """
        SELECT e.* FROM exercises e
        LEFT JOIN user_exercise_state s ON s.exerciseId = e.id
        WHERE COALESCE(s.isHidden, 0) = 0 AND e.deprecated = 0 AND e.deletedAt IS NULL
        ORDER BY e.name ASC
        """
    )
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    /**
     * Get all exercises including hidden ones. Still excludes deleted ones.
     */
    @Query("SELECT * FROM exercises WHERE deprecated = 0 AND deletedAt IS NULL ORDER BY name ASC")
    fun getAllExercisesIncludingHidden(): Flow<List<ExerciseEntity>>

    /**
     * Get a single exercise by ID.
     * Not filtered by hidden or deprecated - a detail lookup must resolve
     * regardless of either status - but excludes deleted rows.
     * @return ExerciseEntity if found, null otherwise.
     */
    @Query("SELECT * FROM exercises WHERE id = :id AND deletedAt IS NULL")
    suspend fun getExerciseById(id: String): ExerciseEntity?

    /**
     * Search exercises by name (case-insensitive).
     * @param query Search query (will be wrapped with % for LIKE query)
     */
    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' AND deprecated = 0 AND deletedAt IS NULL ORDER BY name ASC")
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
     * Tombstone an exercise by ID instead of removing the row, so a future
     * sync engine has something to push. Only meaningful for CUSTOM rows -
     * DEFAULT rows are reference data, replaced wholesale by the seeder.
     */
    @Query("UPDATE exercises SET deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun deleteExercise(id: String, deletedAt: Long, updatedAt: Long)

    /**
     * Delete all exercises.
     * Useful for testing or clearing data. A full wipe, not the per-row
     * tombstone path - unlike deleteExercise, this really removes rows.
     */
    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()

    /**
     * Get exercises filtered by difficulty level. Excludes hidden, deprecated and deleted exercises.
     */
    @Query(
        """
        SELECT e.* FROM exercises e
        LEFT JOIN user_exercise_state s ON s.exerciseId = e.id
        WHERE e.difficulty = :difficulty AND e.deprecated = 0 AND e.deletedAt IS NULL AND COALESCE(s.isHidden, 0) = 0
        ORDER BY e.name ASC
        """
    )
    fun getExercisesByDifficulty(difficulty: String): Flow<List<ExerciseEntity>>

    /**
     * Get exercises that require specific equipment. Excludes hidden, deprecated and deleted exercises.
     * Uses LIKE to match equipment in the comma-separated list.
     */
    @Query(
        """
        SELECT e.* FROM exercises e
        LEFT JOIN user_exercise_state s ON s.exerciseId = e.id
        WHERE e.equipmentNeeded LIKE '%' || :equipment || '%' AND e.deprecated = 0 AND e.deletedAt IS NULL AND COALESCE(s.isHidden, 0) = 0
        ORDER BY e.name ASC
        """
    )
    fun getExercisesByEquipment(equipment: String): Flow<List<ExerciseEntity>>

    /**
     * Get exercises filtered by category. Excludes hidden, deprecated and deleted exercises.
     */
    @Query(
        """
        SELECT e.* FROM exercises e
        LEFT JOIN user_exercise_state s ON s.exerciseId = e.id
        WHERE e.category = :category AND e.deprecated = 0 AND e.deletedAt IS NULL AND COALESCE(s.isHidden, 0) = 0
        ORDER BY e.name ASC
        """
    )
    fun getExercisesByCategory(category: String): Flow<List<ExerciseEntity>>

    /**
     * Get exercises filtered by type (compound vs isolation). Excludes hidden, deprecated and deleted exercises.
     */
    @Query(
        """
        SELECT e.* FROM exercises e
        LEFT JOIN user_exercise_state s ON s.exerciseId = e.id
        WHERE e.exerciseType = :type AND e.deprecated = 0 AND e.deletedAt IS NULL AND COALESCE(s.isHidden, 0) = 0
        ORDER BY e.name ASC
        """
    )
    fun getExercisesByType(type: String): Flow<List<ExerciseEntity>>

    /**
     * Get only custom (user-created) exercises.
     */
    @Query("SELECT * FROM exercises WHERE source = 'CUSTOM' AND deprecated = 0 AND deletedAt IS NULL ORDER BY name ASC")
    fun getCustomExercises(): Flow<List<ExerciseEntity>>

    /**
     * Get only default (preset) exercises.
     */
    @Query("SELECT * FROM exercises WHERE source = 'DEFAULT' AND deprecated = 0 AND deletedAt IS NULL ORDER BY name ASC")
    fun getDefaultExercises(): Flow<List<ExerciseEntity>>

    /**
     * Get exercises that target a specific primary muscle group. Excludes hidden, deprecated and deleted exercises.
     */
    @Query(
        """
        SELECT e.* FROM exercises e
        LEFT JOIN user_exercise_state s ON s.exerciseId = e.id
        WHERE e.primaryMuscles LIKE '%' || :muscleGroup || '%' AND e.deprecated = 0 AND e.deletedAt IS NULL AND COALESCE(s.isHidden, 0) = 0
        ORDER BY e.name ASC
        """
    )
    fun getExercisesByPrimaryMuscle(muscleGroup: String): Flow<List<ExerciseEntity>>

    /**
     * Get count of all exercises.
     * Useful for checking if database needs seeding.
     */
    @Query("SELECT COUNT(*) FROM exercises WHERE deprecated = 0 AND deletedAt IS NULL")
    suspend fun getExerciseCount(): Int

    /**
     * Get count of default exercises.
     */
    @Query("SELECT COUNT(*) FROM exercises WHERE source = 'DEFAULT' AND deprecated = 0 AND deletedAt IS NULL")
    suspend fun getDefaultExerciseCount(): Int

    /**
     * Insert multiple exercises at once.
     * Useful for bulk seeding default exercises.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    /**
     * Delete all default exercises.
     * Useful when updating default exercise library. A full wipe of
     * reference data, not the per-row tombstone path - DEFAULT rows aren't
     * user-owned, so they're replaced wholesale rather than tombstoned.
     */
    @Query("DELETE FROM exercises WHERE source = 'DEFAULT'")
    suspend fun deleteAllDefaultExercises()

    /**
     * Get all hidden exercises. Requires an overlay row marking the
     * exercise hidden, so orphaned overlay rows (exercise no longer
     * exists) never appear here. Excludes deleted exercises.
     */
    @Query(
        """
        SELECT e.* FROM exercises e
        INNER JOIN user_exercise_state s ON s.exerciseId = e.id
        WHERE s.isHidden = 1 AND e.deprecated = 0 AND e.deletedAt IS NULL
        ORDER BY e.name ASC
        """
    )
    fun getHiddenExercises(): Flow<List<ExerciseEntity>>
}
