package com.rukavina.gymbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rukavina.gymbuddy.data.local.entity.TemplateVersionEntity

/**
 * DAO for managing workout template version tracking.
 * Used to determine when to update default workout templates.
 */
@Dao
interface TemplateVersionDao {

    /**
     * Get the current template version.
     * Returns null if no version has been loaded yet (first app launch).
     */
    @Query("SELECT * FROM template_version WHERE id = 1")
    suspend fun getCurrentVersion(): TemplateVersionEntity?

    /**
     * Set or update the current template version.
     * Uses REPLACE strategy to update the single row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setVersion(version: TemplateVersionEntity)

    /**
     * Clear the version (for testing or manual refresh).
     */
    @Query("DELETE FROM template_version")
    suspend fun clearVersion()
}
