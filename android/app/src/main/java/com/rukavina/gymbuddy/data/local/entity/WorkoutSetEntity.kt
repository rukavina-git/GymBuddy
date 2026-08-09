package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.rukavina.gymbuddy.data.local.converter.SetTypeConverter
import com.rukavina.gymbuddy.domain.model.SetType

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
@TypeConverters(SetTypeConverter::class)
data class WorkoutSetEntity(
    @PrimaryKey
    val id: String,
    val performedExerciseId: String,
    val weightKg: Float? = null,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
    val distanceMeters: Float? = null,
    val setType: SetType = SetType.WORKING,
    val isCompleted: Boolean = false,
    val restTakenSeconds: Int? = null,
    val orderIndex: Int
)
