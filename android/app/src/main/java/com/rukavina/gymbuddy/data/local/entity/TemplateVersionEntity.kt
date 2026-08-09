package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to track the version of default workout templates loaded into the database.
 * Used to determine if default templates need to be updated.
 */
@Entity(tableName = "template_version")
data class TemplateVersionEntity(
    @PrimaryKey
    val id: Int = 1, // Single row table, always ID = 1

    /**
     * Version number of the currently loaded default templates.
     * Compared against bundled version to check for updates.
     */
    val version: Int,

    /**
     * Timestamp when this version was loaded (millis since epoch).
     */
    val loadedAt: Long = System.currentTimeMillis()
)
