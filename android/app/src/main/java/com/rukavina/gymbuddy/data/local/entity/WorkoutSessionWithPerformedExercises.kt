package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room relation class for querying WorkoutSession with its PerformedExercises.
 * Used for one-to-many relationship queries.
 */
data class WorkoutSessionWithPerformedExercises(
    @Embedded
    val workoutSession: WorkoutSessionEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "workoutSessionId"
    )
    val performedExercises: List<PerformedExerciseEntity>
)
