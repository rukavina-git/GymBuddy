package com.rukavina.gymbuddy.api.dto

import com.rukavina.gymbuddy.domain.ActivityLevel
import com.rukavina.gymbuddy.domain.DifficultyLevel
import com.rukavina.gymbuddy.domain.EntitySource
import com.rukavina.gymbuddy.domain.EntityType
import com.rukavina.gymbuddy.domain.Equipment
import com.rukavina.gymbuddy.domain.ExerciseCategory
import com.rukavina.gymbuddy.domain.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.ExerciseType
import com.rukavina.gymbuddy.domain.FitnessGoal
import com.rukavina.gymbuddy.domain.Gender
import com.rukavina.gymbuddy.domain.MuscleGroup
import com.rukavina.gymbuddy.domain.SetType
import com.rukavina.gymbuddy.domain.SyncStatus

/**
 * Request-side shapes for POST /v1/sync/push. Deliberately distinct
 * from the response-side ExerciseDto/WorkoutTemplateDto/TemplateExerciseDto/
 * UserProfileDto in ReferenceDtos.kt/ProfileDtos.kt: those model
 * updatedAt/revision (and ownerId) as always-present, server-assigned
 * facts about a stored row, which is correct for a response but wrong
 * for a request - the push examples in api/openapi.yaml never include
 * them, and ownership must come from the security context, never the
 * payload (see auth/CurrentUser.kt). revision is the one server field a
 * client does send: it's the client's last-known revision, the basis
 * for the optimistic-concurrency check in sync/RevisionChecker.kt.
 *
 * WorkoutSession/Exercise/WorkoutTemplate additionally carry a
 * deletedAt the client CAN set: per api/openapi.yaml, any non-null
 * value is a delete signal, not a timestamp the server trusts - the
 * service always substitutes its own current time on write. See the
 * forComparison() extensions below for how this is normalised before
 * the idempotency content-equality check.
 */

data class WorkoutSetSyncDto(
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

data class PerformedExerciseSyncDto(
    val id: String,
    val exerciseId: String,
    val orderIndex: Int,
    val exerciseName: String,
    val exerciseCategory: ExerciseCategory,
    val exerciseTrackingType: ExerciseTrackingType,
    val exercisePrimaryMuscles: List<MuscleGroup>,
    val sets: List<WorkoutSetSyncDto>,
    val supersetGroup: Int? = null,
)

data class WorkoutSessionSyncDto(
    val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val durationSeconds: Int,
    val title: String,
    val notes: String?,
    val templateId: String?,
    val templateTitle: String?,
    val performedExercises: List<PerformedExerciseSyncDto>,
    val revision: Int = 0,
    val deletedAt: Long? = null,
)

data class TemplateExerciseSyncDto(
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

data class WorkoutTemplateSyncDto(
    val id: String,
    val title: String,
    val templateExercises: List<TemplateExerciseSyncDto>,
    val source: EntitySource,
    val derivedFromId: String? = null,
    val deprecated: Boolean = false,
    val revision: Int = 0,
    val deletedAt: Long? = null,
)

data class ExerciseSyncDto(
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
    val derivedFromId: String? = null,
    val deprecated: Boolean = false,
    val revision: Int = 0,
    val deletedAt: Long? = null,
)

data class UserExerciseStateSyncDto(
    val exerciseId: String,
    val isHidden: Boolean,
    val isFavorite: Boolean,
    val note: String?,
    val defaultRestSeconds: Int?,
    val revision: Int = 0,
)

data class UserTemplateStateSyncDto(
    val templateId: String,
    val isHidden: Boolean,
    val isFavorite: Boolean,
    val revision: Int = 0,
)

data class UserProfileSyncDto(
    val name: String,
    val email: String,
    val profileImageUrl: String?,
    val birthDate: Long?,
    val weight: Float?,
    val height: Float?,
    val gender: Gender?,
    val fitnessGoal: FitnessGoal?,
    val activityLevel: ActivityLevel?,
    val targetWeight: Float?,
    val joinedDate: Long,
    val bio: String?,
    val revision: Int = 0,
)

/**
 * Placeholder swapped in for deletedAt before the idempotency
 * content-equality check in sync/RevisionChecker.kt. The server never
 * trusts the client's exact deletedAt value (see the header above) -
 * only whether it's null - so comparing raw values would make a
 * genuine retry of a delete look like a content mismatch (the client
 * resends the same local delete timestamp, but the previously-stored
 * row now holds the server's stamped time instead). Normalising both
 * sides to this fixed marker when non-null makes only "deleted vs not"
 * significant, matching what the server actually persists.
 */
const val DELETED_CONTENT_MARKER = 0L

fun WorkoutSessionSyncDto.forComparison(): WorkoutSessionSyncDto =
    copy(revision = 0, deletedAt = deletedAt?.let { DELETED_CONTENT_MARKER })

fun ExerciseSyncDto.forComparison(): ExerciseSyncDto =
    copy(revision = 0, deletedAt = deletedAt?.let { DELETED_CONTENT_MARKER })

fun WorkoutTemplateSyncDto.forComparison(): WorkoutTemplateSyncDto =
    copy(revision = 0, deletedAt = deletedAt?.let { DELETED_CONTENT_MARKER })

/** Mirrors the PushRequest schema in api/openapi.yaml. */
data class PushRequestDto(
    val workoutSessions: List<WorkoutSessionSyncDto> = emptyList(),
    val exercises: List<ExerciseSyncDto> = emptyList(),
    val workoutTemplates: List<WorkoutTemplateSyncDto> = emptyList(),
    val userExerciseStates: List<UserExerciseStateSyncDto> = emptyList(),
    val userTemplateStates: List<UserTemplateStateSyncDto> = emptyList(),
    val userProfile: UserProfileSyncDto? = null,
)

/** Mirrors the MutationResult schema in api/openapi.yaml. */
data class MutationResultDto(
    val entityType: EntityType,
    val entityId: String,
    val status: SyncStatus,
    val updatedAt: Long? = null,
    val revision: Int? = null,
    val reason: String? = null,
)

/** Mirrors the PushResponse schema in api/openapi.yaml. */
data class PushResponseDto(val results: List<MutationResultDto>)
