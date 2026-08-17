package com.rukavina.gymbuddy.api.dto

import com.rukavina.gymbuddy.domain.ExerciseCategory
import com.rukavina.gymbuddy.domain.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.MuscleGroup
import com.rukavina.gymbuddy.domain.SetType

/**
 * Response-side shapes for GET /v1/sync/pull. Distinct from the
 * push-side WorkoutSessionSyncDto/PerformedExerciseSyncDto/
 * WorkoutSetSyncDto/UserExerciseStateSyncDto/UserTemplateStateSyncDto in
 * SyncDtos.kt for the same reason ExerciseDto/WorkoutTemplateDto/
 * UserProfileDto (reused here directly from ReferenceDtos.kt/
 * ProfileDtos.kt) are distinct from their Sync counterparts: updatedAt/
 * revision (and here, deletedAt) are always-present, server-assigned
 * facts about a stored row on the way out, never optional the way they
 * are on the way in.
 */

data class WorkoutSetDto(
    val id: String,
    val weightKg: Float?,
    val reps: Int?,
    val durationSeconds: Int?,
    val distanceMeters: Float?,
    val setType: SetType,
    val isCompleted: Boolean,
    val restTakenSeconds: Int?,
    val orderIndex: Int,
)

data class PerformedExerciseDto(
    val id: String,
    val exerciseId: String,
    val orderIndex: Int,
    val exerciseName: String,
    val exerciseCategory: ExerciseCategory,
    val exerciseTrackingType: ExerciseTrackingType,
    val exercisePrimaryMuscles: List<MuscleGroup>,
    val sets: List<WorkoutSetDto>,
    val supersetGroup: Int?,
)

/** Mirrors the WorkoutSession schema in api/openapi.yaml. */
data class WorkoutSessionDto(
    val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val durationSeconds: Int,
    val title: String,
    val notes: String?,
    val templateId: String?,
    val templateTitle: String?,
    val performedExercises: List<PerformedExerciseDto>,
    val updatedAt: Long,
    val deletedAt: Long?,
    val revision: Int,
)

/** Mirrors the UserExerciseState schema in api/openapi.yaml. */
data class UserExerciseStateDto(
    val exerciseId: String,
    val isHidden: Boolean,
    val isFavorite: Boolean,
    val note: String?,
    val defaultRestSeconds: Int?,
    val updatedAt: Long,
    val revision: Int,
)

/** Mirrors the UserTemplateState schema in api/openapi.yaml. */
data class UserTemplateStateDto(
    val templateId: String,
    val isHidden: Boolean,
    val isFavorite: Boolean,
    val updatedAt: Long,
    val revision: Int,
)

/**
 * Mirrors the PullResponse schema in api/openapi.yaml. exercises/
 * workoutTemplates/userProfile reuse the response DTOs from
 * ReferenceDtos.kt/ProfileDtos.kt directly - same shape, same source of
 * truth, no reason to duplicate them here.
 */
data class PullResponseDto(
    val workoutSessions: List<WorkoutSessionDto> = emptyList(),
    val exercises: List<ExerciseDto> = emptyList(),
    val workoutTemplates: List<WorkoutTemplateDto> = emptyList(),
    val userExerciseStates: List<UserExerciseStateDto> = emptyList(),
    val userTemplateStates: List<UserTemplateStateDto> = emptyList(),
    val userProfile: UserProfileDto? = null,
    val nextCursor: String,
    val hasMore: Boolean,
)
