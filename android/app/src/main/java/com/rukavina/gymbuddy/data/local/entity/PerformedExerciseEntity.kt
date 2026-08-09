package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import androidx.room.Embedded
import androidx.room.Relation

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
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workoutSessionId"]),
        Index(value = ["exerciseId"])
    ]
)
data class PerformedExerciseEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val workoutSessionId: String,
    val exerciseId: Int
)

/**
 * Relation object for PerformedExercise with its sets.
 */
data class PerformedExerciseWithSets(
    @Embedded val performedExercise: PerformedExerciseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "performedExerciseId"
    )
    val sets: List<WorkoutSetEntity>
)
