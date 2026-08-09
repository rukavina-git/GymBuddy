package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for WorkoutSession table.
 * PerformedExercises are stored in a separate table with foreign key relationship.
 *
 * templateId is a soft reference to the workout_templates table, not a
 * foreign key: it must tolerate the referenced template being deleted.
 * templateTitle is a snapshot of WorkoutTemplateEntity.title written once
 * and never updated afterward, same rule as the PerformedExercise snapshot
 * columns.
 */
@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey
    val id: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val durationSeconds: Int,
    val title: String,
    val notes: String? = null,
    val templateId: String? = null,
    val templateTitle: String? = null
)
