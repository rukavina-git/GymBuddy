package com.rukavina.gymbuddy.api.dto

import com.rukavina.gymbuddy.domain.DifficultyLevel
import com.rukavina.gymbuddy.domain.EntitySource
import com.rukavina.gymbuddy.domain.Equipment
import com.rukavina.gymbuddy.domain.ExerciseCategory
import com.rukavina.gymbuddy.domain.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.ExerciseType
import com.rukavina.gymbuddy.domain.MuscleGroup

/** Mirrors the ReferenceVersion schema in api/openapi.yaml. */
data class ReferenceVersionDto(
    val exerciseLibraryVersion: Int,
    val templateLibraryVersion: Int,
)

/** Mirrors the Exercise schema in api/openapi.yaml. */
data class ExerciseDto(
    val id: String,
    val name: String,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup>,
    val description: String?,
    val instructions: List<String>,
    val tips: List<String>,
    val difficulty: DifficultyLevel,
    val equipmentNeeded: List<Equipment>,
    val category: ExerciseCategory,
    val exerciseType: ExerciseType,
    val trackingType: ExerciseTrackingType,
    val videoUrl: String?,
    val thumbnailUrl: String?,
    val source: EntitySource,
    val ownerId: String?,
    val derivedFromId: String?,
    val deprecated: Boolean,
    val updatedAt: Long,
    val deletedAt: Long?,
    val revision: Int,
)

/** Mirrors the ExerciseLibraryResponse schema in api/openapi.yaml. */
data class ExerciseLibraryResponseDto(
    val version: Int,
    val exercises: List<ExerciseDto>,
)

/** Mirrors the TemplateExercise schema in api/openapi.yaml. */
data class TemplateExerciseDto(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val exerciseTrackingType: ExerciseTrackingType,
    val plannedSets: Int,
    val plannedReps: Int?,
    val orderIndex: Int,
    val restSeconds: Int?,
    val plannedDurationSeconds: Int?,
    val plannedDistanceMeters: Float?,
    val plannedWeightKg: Float?,
    val notes: String?,
)

/** Mirrors the WorkoutTemplate schema in api/openapi.yaml. */
data class WorkoutTemplateDto(
    val id: String,
    val title: String,
    val templateExercises: List<TemplateExerciseDto>,
    val source: EntitySource,
    val ownerId: String?,
    val derivedFromId: String?,
    val deprecated: Boolean,
    val updatedAt: Long,
    val deletedAt: Long?,
    val revision: Int,
)

/** Mirrors the TemplateLibraryResponse schema in api/openapi.yaml. */
data class TemplateLibraryResponseDto(
    val version: Int,
    val templates: List<WorkoutTemplateDto>,
)
