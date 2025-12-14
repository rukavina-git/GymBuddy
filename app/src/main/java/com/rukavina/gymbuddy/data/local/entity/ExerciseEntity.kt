package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.rukavina.gymbuddy.data.local.converter.MuscleGroupConverter
import com.rukavina.gymbuddy.data.model.MuscleGroup

/**
 * Room entity for Exercise table.
 * Separate from domain model to keep domain layer clean.
 */
@Entity(tableName = "exercises")
@TypeConverters(MuscleGroupConverter::class)
data class ExerciseEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup>
)
