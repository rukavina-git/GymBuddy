package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for WorkoutSession table.
 * PerformedExercises are stored in a separate table with foreign key relationship.
 */
@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey
    val id: String,
    val date: Long,
    val durationSeconds: Int,
    val title: String
)
