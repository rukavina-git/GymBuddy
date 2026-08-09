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
 * isHidden/note. Detail lookups by id deliberately skip the join - they
 * must resolve regardless of hidden/deprecated status.
 */
@Dao
interface ExerciseDao {
    /**
     * Get all exercises as a Flow for reactive updates.
     * Ordered alphabetically by name.
     * Excludes hidden exercises.
     */
    @Query(
        """
        SELECT e.* FROM exercises e
        LEFT JOIN user_exercise_state s ON s.exerciseId = e.id
        WHERE COALESCE(s.isHidden, 0) = 0 AND e.deprecated = 0
        ORDER BY e.name ASC
        """
    )
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    /**
     * Get all exercises including hidden ones.
     */
    @Query("SELECT * FROM exercises WHERE deprecated = 0 ORDER BY name ASC")
    fun getAllExercisesIncludingHidden(): Flow<List<ExerciseEntity>>

    /**
     * Get a single exercise by ID.
     * Not filtered by hidden or deprecated - a detail lookup must resolve
     * regardless of either status.
     * @return ExerciseEntity if found, null otherwise.
     */
    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: String): ExerciseEntity?

    /**
     * Search exercises by name (case-insensitive).
     * @param query Search query (will be wrapped with % for LIKE query)
     */
    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' AND deprecated = 0 ORDER BY name ASC")
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

    /**
     * Get exercises filtered by difficulty level. Excludes hidden exercises.
     */
    @Query(
        """
        SELECT e.* FROM exercises e
        LEFT JOIN user_exercise_state s ON s.exerciseId = e.id
        WHERE e.difficulty = :difficulty AND e.deprecated = 0 AND COALESCE(s.isHidden, 0) = 0
        ORDER BY e.name ASC
        """
    )
    fun getExercisesByDifficulty(difficulty: String): Flow<List<ExerciseEntity>>

    /**
     * Get exercises that require specific equipment. Excludes hidden exercises.
     * Uses LIKE to match equipment in the comma-separated list.
     */
    @Query(
        """
        SELECT e.* FROM exercises e
        LEFT JOIN user_exercise_state s ON s.exerciseId = e.id
        WHERE e.equipmentNeeded LIKE '%' || :equipment || '%' AND e.deprecated = 0 AND COALESCE(s.isHidden, 0) = 0
        ORDER BY e.name ASC
        """
    )
    fun getExercisesByEquipment(equipment: String): Flow<List<ExerciseEntity>>

    /**
     * Get exercises filtered by category. Excludes hidden exercises.
     */
    @Query(
        """
        SELECT e.* FROM exercises e
        LEFT JOIN user_exercise_state s ON s.exerciseId = e.id
        WHERE e.category = :category AND e.deprecated = 0 AND COALESCE(s.isHidden, 0) = 0
        ORDER BY e.name ASC
        """
    )
    fun getExercisesByCategory(category: String): Flow<List<ExerciseEntity>>

    /**
     * Get exercises filtered by type (compound vs isolation). Excludes hidden exercises.
     */
    @Query(
        """
        SELECT e.* FROM exercises e
        LEFT JOIN user_exercise_state s ON s.exerciseId = e.id
        WHERE e.exerciseType = :type AND e.deprecated = 0 AND COALESCE(s.isHidden, 0) = 0
        ORDER BY e.name ASC
        """
    )
    fun getExercisesByType(type: String): Flow<List<ExerciseEntity>>

    /**
     * Get only custom (user-created) exercises.
     */
    @Query("SELECT * FROM exercises WHERE source = 'CUSTOM' AND deprecated = 0 ORDER BY name ASC")
    fun getCustomExercises(): Flow<List<ExerciseEntity>>

    /**
     * Get only default (preset) exercises.
     */
    @Query("SELECT * FROM exercises WHERE source = 'DEFAULT' AND deprecated = 0 ORDER BY name ASC")
    fun getDefaultExercises(): Flow<List<ExerciseEntity>>

    /**
     * Get exercises that target a specific primary muscle group. Excludes hidden exercises.
     */
    @Query(
        """
        SELECT e.* FROM exercises e
        LEFT JOIN user_exercise_state s ON s.exerciseId = e.id
        WHERE e.primaryMuscles LIKE '%' || :muscleGroup || '%' AND e.deprecated = 0 AND COALESCE(s.isHidden, 0) = 0
        ORDER BY e.name ASC
        """
    )
    fun getExercisesByPrimaryMuscle(muscleGroup: String): Flow<List<ExerciseEntity>>

    /**
     * Get count of all exercises.
     * Useful for checking if database needs seeding.
     */
    @Query("SELECT COUNT(*) FROM exercises WHERE deprecated = 0")
    suspend fun getExerciseCount(): Int

    /**
     * Get count of default exercises.
     */
    @Query("SELECT COUNT(*) FROM exercises WHERE source = 'DEFAULT' AND deprecated = 0")
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
    @Query("DELETE FROM exercises WHERE source = 'DEFAULT'")
    suspend fun deleteAllDefaultExercises()

    /**
     * Get all hidden exercises. Requires an overlay row marking the
     * exercise hidden, so orphaned overlay rows (exercise no longer
     * exists) never appear here.
     */
    @Query(
        """
        SELECT e.* FROM exercises e
        INNER JOIN user_exercise_state s ON s.exerciseId = e.id
        WHERE s.isHidden = 1 AND e.deprecated = 0
        ORDER BY e.name ASC
        """
    )
    fun getHiddenExercises(): Flow<List<ExerciseEntity>>
}
