package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.MuscleGroup

/**
 * Room entity for PerformedExercise table.
 * Has foreign key relationship with WorkoutSession (cascade delete).
 *
 * exerciseId is a soft reference to the exercises table, not a foreign
 * key: it must tolerate the referenced exercise being re-seeded,
 * deprecated, or (for a custom exercise) deleted entirely. History reads
 * must use the exerciseName/exerciseCategory/exerciseTrackingType/
 * exercisePrimaryMuscles snapshot columns, never a join back to
 * exercises, so a library update or rename cannot rewrite history.
 *
 * Those four snapshot columns are written once at creation and never
 * updated afterward.
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
    val id: String,
    val workoutSessionId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val exerciseName: String,
    val exerciseCategory: ExerciseCategory,
    val exerciseTrackingType: ExerciseTrackingType,
    val exercisePrimaryMuscles: List<MuscleGroup>
)

/**
 * Relation object for PerformedExercise with its sets.
 * Sets are populated by the DAO in orderIndex order (see WorkoutSessionDao) -
 * not by Room's @Relation, which does not support ORDER BY.
 */
data class PerformedExerciseWithSets(
    val performedExercise: PerformedExerciseEntity,
    val sets: List<WorkoutSetEntity>
)
