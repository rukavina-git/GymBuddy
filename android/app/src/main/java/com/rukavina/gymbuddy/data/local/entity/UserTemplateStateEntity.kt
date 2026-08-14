package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rukavina.gymbuddy.domain.model.SyncState

/**
 * Per-user overlay state for a WorkoutTemplate: hidden/favorite flags.
 * See UserExerciseStateEntity for the full rationale - same sparse,
 * upsert-only, soft-reference design, applied to templates instead of
 * exercises. Also gets updatedAt/revision/syncState but no deletedAt,
 * for the same reason.
 */
@Entity(tableName = "user_template_state")
data class UserTemplateStateEntity(
    @PrimaryKey
    val templateId: String,
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false,
    val updatedAt: Long = 0L,
    val revision: Int = 0,
    val syncState: SyncState = SyncState.PENDING
)
