package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.rukavina.gymbuddy.data.local.converter.ExerciseConverters
import com.rukavina.gymbuddy.data.local.converter.MuscleGroupConverter
import com.rukavina.gymbuddy.data.model.DifficultyLevel
import com.rukavina.gymbuddy.data.model.Equipment
import com.rukavina.gymbuddy.data.model.ExerciseCategory
import com.rukavina.gymbuddy.data.model.ExerciseType
import com.rukavina.gymbuddy.data.model.MuscleGroup

/**
 * Room entity for Exercise table.
 * Separate from domain model to keep domain layer clean.
 */
@Entity(tableName = "exercises")
@TypeConverters(MuscleGroupConverter::class, ExerciseConverters::class)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val name: String,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup>,
    val description: String?,
    val instructions: List<String>,
    val tips: List<String>,
    val note: String?,
    val difficulty: DifficultyLevel,
    val equipmentNeeded: List<Equipment>,
    val category: ExerciseCategory,
    val exerciseType: ExerciseType,
    val videoUrl: String?,
    val thumbnailUrl: String?,
    val isCustom: Boolean,
    val createdBy: String?,
    val isHidden: Boolean
)
