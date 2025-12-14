package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for PerformedExercise table.
 * Has foreign key relationship with WorkoutSession (cascade delete).
 * Has reference to Exercise (no cascade - exercises can exist independently).
 */
@Entity(
    tableName = "performed_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutSessionId"],
            onDelete = ForeignKey.CASCADE // Delete performed exercises when workout session is deleted
        )
    ],
    indices = [
        Index(value = ["workoutSessionId"]), // Index for faster queries by workout session
        Index(value = ["exerciseId"]) // Index for faster queries by exercise
    ]
)
data class PerformedExerciseEntity(
    @PrimaryKey
    val id: String,
    val workoutSessionId: String, // Foreign key to WorkoutSessionEntity
    val exerciseId: String, // Reference to ExerciseEntity
    val weight: Float,
    val reps: Int,
    val sets: Int
)
