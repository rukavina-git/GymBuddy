package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-user overlay state for a WorkoutTemplate: hidden/favorite flags.
 * See UserExerciseStateEntity for the full rationale - same sparse,
 * upsert-only, soft-reference design, applied to templates instead of
 * exercises.
 */
@Entity(tableName = "user_template_state")
data class UserTemplateStateEntity(
    @PrimaryKey
    val templateId: String,
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false
)
