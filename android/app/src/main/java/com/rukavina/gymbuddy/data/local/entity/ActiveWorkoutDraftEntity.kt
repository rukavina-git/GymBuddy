package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The one in-progress workout (always id 0), stored as JSON so the
 * draft's shape can change without a migration. Local only: never in
 * the outbox, never synced. Wiped with the rest of the user's data.
 */
@Entity(tableName = "active_workout_draft")
data class ActiveWorkoutDraftEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val payloadJson: String,
    val updatedAt: Long
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}
