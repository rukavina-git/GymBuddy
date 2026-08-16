package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.api.dto.ExerciseSyncDto
import com.rukavina.gymbuddy.api.dto.PerformedExerciseSyncDto
import com.rukavina.gymbuddy.api.dto.TemplateExerciseSyncDto
import com.rukavina.gymbuddy.api.dto.UserExerciseStateSyncDto
import com.rukavina.gymbuddy.api.dto.UserProfileSyncDto
import com.rukavina.gymbuddy.api.dto.UserTemplateStateSyncDto
import com.rukavina.gymbuddy.api.dto.WorkoutSessionSyncDto
import com.rukavina.gymbuddy.api.dto.WorkoutSetSyncDto
import com.rukavina.gymbuddy.api.dto.WorkoutTemplateSyncDto
import com.rukavina.gymbuddy.domain.DifficultyLevel
import com.rukavina.gymbuddy.domain.EntitySource
import com.rukavina.gymbuddy.domain.ExerciseCategory
import com.rukavina.gymbuddy.domain.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.ExerciseType
import com.rukavina.gymbuddy.domain.MuscleGroup
import com.rukavina.gymbuddy.domain.SetType
import java.util.UUID

/** Minimal-but-valid entity builders shared by the sync push tests. */
object SyncTestFixtures {

    fun uuid(): String = UUID.randomUUID().toString()

    fun workoutSet(
        id: String = uuid(),
        reps: Int? = 8,
        weightKg: Float? = 50f,
        orderIndex: Int = 0,
    ) = WorkoutSetSyncDto(
        id = id,
        weightKg = weightKg,
        reps = reps,
        durationSeconds = null,
        distanceMeters = null,
        setType = SetType.WORKING,
        isCompleted = true,
        restTakenSeconds = 60,
        orderIndex = orderIndex,
    )

    fun performedExercise(
        id: String = uuid(),
        exerciseId: String = uuid(),
        sets: List<WorkoutSetSyncDto> = listOf(workoutSet()),
        orderIndex: Int = 0,
    ) = PerformedExerciseSyncDto(
        id = id,
        exerciseId = exerciseId,
        orderIndex = orderIndex,
        exerciseName = "Barbell Bench Press",
        exerciseCategory = ExerciseCategory.STRENGTH,
        exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
        exercisePrimaryMuscles = listOf(MuscleGroup.CHEST),
        sets = sets,
        supersetGroup = null,
    )

    fun workoutSession(
        id: String = uuid(),
        title: String = "Push Day",
        revision: Int = 0,
        performedExercises: List<PerformedExerciseSyncDto> = listOf(performedExercise()),
    ) = WorkoutSessionSyncDto(
        id = id,
        startedAt = 1_700_000_000_000,
        endedAt = 1_700_000_600_000,
        durationSeconds = 600,
        title = title,
        notes = null,
        templateId = null,
        templateTitle = null,
        performedExercises = performedExercises,
        revision = revision,
    )

    fun exercise(
        id: String = uuid(),
        name: String = "Custom Curl",
        source: EntitySource = EntitySource.CUSTOM,
        revision: Int = 0,
    ) = ExerciseSyncDto(
        id = id,
        name = name,
        primaryMuscles = listOf(MuscleGroup.ARMS),
        secondaryMuscles = emptyList(),
        description = null,
        instructions = emptyList(),
        tips = emptyList(),
        difficulty = DifficultyLevel.BEGINNER,
        equipmentNeeded = emptyList(),
        category = ExerciseCategory.STRENGTH,
        exerciseType = ExerciseType.ISOLATION,
        trackingType = ExerciseTrackingType.WEIGHT_REPS,
        videoUrl = null,
        thumbnailUrl = null,
        source = source,
        derivedFromId = null,
        deprecated = false,
        revision = revision,
    )

    fun templateExercise(
        id: String = uuid(),
        exerciseId: String = uuid(),
        plannedSets: Int = 3,
        orderIndex: Int = 0,
    ) = TemplateExerciseSyncDto(
        id = id,
        exerciseId = exerciseId,
        exerciseName = "Barbell Bench Press",
        exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
        plannedSets = plannedSets,
        plannedReps = 8,
        orderIndex = orderIndex,
        restSeconds = 90,
        plannedDurationSeconds = null,
        plannedDistanceMeters = null,
        plannedWeightKg = null,
        notes = null,
    )

    fun workoutTemplate(
        id: String = uuid(),
        title: String = "Push Day Template",
        revision: Int = 0,
        templateExercises: List<TemplateExerciseSyncDto> = listOf(templateExercise()),
    ) = WorkoutTemplateSyncDto(
        id = id,
        title = title,
        templateExercises = templateExercises,
        source = EntitySource.CUSTOM,
        derivedFromId = null,
        deprecated = false,
        revision = revision,
    )

    fun userExerciseState(exerciseId: String = uuid(), revision: Int = 0) = UserExerciseStateSyncDto(
        exerciseId = exerciseId,
        isHidden = false,
        isFavorite = true,
        note = null,
        defaultRestSeconds = 90,
        revision = revision,
    )

    fun userTemplateState(templateId: String = uuid(), revision: Int = 0) = UserTemplateStateSyncDto(
        templateId = templateId,
        isHidden = false,
        isFavorite = true,
        revision = revision,
    )

    fun userProfile(name: String = "Test User", revision: Int = 0) = UserProfileSyncDto(
        name = name,
        email = "test@example.com",
        profileImageUrl = null,
        birthDate = null,
        weight = 80f,
        height = 180f,
        gender = null,
        fitnessGoal = null,
        activityLevel = null,
        targetWeight = null,
        joinedDate = 1_700_000_000_000,
        bio = null,
        revision = revision,
    )
}
