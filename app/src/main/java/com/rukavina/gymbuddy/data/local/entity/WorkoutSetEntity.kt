package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for WorkoutSet table.
 * Has foreign key relationship with PerformedExercise (cascade delete).
 */
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = PerformedExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["performedExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["performedExerciseId"])
    ]
)
data class WorkoutSetEntity(
    @PrimaryKey
    val id: String,
    val performedExerciseId: String,
    val weight: Float,
    val reps: Int,
    val orderIndex: Int
)
