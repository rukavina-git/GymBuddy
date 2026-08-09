package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to track the version of default exercises loaded into the database.
 * Used to determine if default exercises need to be updated.
 */
@Entity(tableName = "exercise_version")
data class ExerciseVersionEntity(
    @PrimaryKey
    val id: Int = 1, // Single row table, always ID = 1

    /**
     * Version number of the currently loaded default exercises.
     * Compared against bundled/backend version to check for updates.
     */
    val version: Int,

    /**
     * Timestamp when this version was loaded (millis since epoch).
     */
    val loadedAt: Long = System.currentTimeMillis()
)
