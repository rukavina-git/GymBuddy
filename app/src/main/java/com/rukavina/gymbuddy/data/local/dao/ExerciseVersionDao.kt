package com.rukavina.gymbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rukavina.gymbuddy.data.local.entity.ExerciseVersionEntity

/**
 * DAO for managing exercise version tracking.
 * Used to determine when to update default exercises.
 */
@Dao
interface ExerciseVersionDao {

    /**
     * Get the current exercise version.
     * Returns null if no version has been loaded yet (first app launch).
     */
    @Query("SELECT * FROM exercise_version WHERE id = 1")
    suspend fun getCurrentVersion(): ExerciseVersionEntity?

    /**
     * Set or update the current exercise version.
     * Uses REPLACE strategy to update the single row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setVersion(version: ExerciseVersionEntity)

    /**
     * Clear the version (for testing or manual refresh).
     */
    @Query("DELETE FROM exercise_version")
    suspend fun clearVersion()
}
