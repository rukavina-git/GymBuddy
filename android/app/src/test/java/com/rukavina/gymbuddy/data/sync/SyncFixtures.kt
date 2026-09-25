package com.rukavina.gymbuddy.data.sync

import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.Equipment
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.domain.model.PerformedExercise
import com.rukavina.gymbuddy.domain.model.TemplateExercise
import com.rukavina.gymbuddy.domain.model.UserProfile
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutSet
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import java.util.UUID

/** Valid domain objects with real UUID ids - the push path rejects anything else. */
object SyncFixtures {

    fun id(): String = UUID.randomUUID().toString()

    fun set(orderIndex: Int = 0, reps: Int = 8, weightKg: Float = 60f) = WorkoutSet(
        id = id(),
        weightKg = weightKg,
        reps = reps,
        isCompleted = true,
        restTakenSeconds = 90,
        orderIndex = orderIndex
    )

    fun performedExercise(sets: List<WorkoutSet> = listOf(set()), orderIndex: Int = 0) = PerformedExercise(
        id = id(),
        exerciseId = id(),
        orderIndex = orderIndex,
        exerciseName = "Bench Press",
        exerciseCategory = ExerciseCategory.STRENGTH,
        exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
        exercisePrimaryMuscles = listOf(MuscleGroup.CHEST),
        sets = sets
    )

    fun session(
        title: String = "Push Day",
        performedExercises: List<PerformedExercise> = listOf(performedExercise())
    ) = WorkoutSession(
        id = id(),
        startedAt = 1_700_000_000_000,
        endedAt = 1_700_000_600_000,
        durationSeconds = 600,
        title = title,
        notes = "felt strong",
        performedExercises = performedExercises
    )

    fun exercise(name: String = "Custom Curl") = Exercise(
        id = id(),
        name = name,
        primaryMuscles = listOf(MuscleGroup.ARMS),
        secondaryMuscles = emptyList(),
        difficulty = DifficultyLevel.BEGINNER,
        equipmentNeeded = listOf(Equipment.DUMBBELL),
        category = ExerciseCategory.STRENGTH,
        exerciseType = ExerciseType.ISOLATION,
        trackingType = ExerciseTrackingType.WEIGHT_REPS
    )

    fun templateExercise(orderIndex: Int = 0, plannedSets: Int = 3) = TemplateExercise(
        id = id(),
        exerciseId = id(),
        exerciseName = "Bench Press",
        exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
        plannedSets = plannedSets,
        plannedReps = 8,
        orderIndex = orderIndex,
        restSeconds = 90
    )

    fun template(
        title: String = "Push Template",
        templateExercises: List<TemplateExercise> = listOf(templateExercise())
    ) = WorkoutTemplate(
        id = id(),
        title = title,
        templateExercises = templateExercises
    )

    fun profile(uid: String, name: String = "Test User") = UserProfile(
        uid = uid,
        name = name,
        email = "test@example.com",
        weight = 80f,
        height = 180f,
        joinedDate = 1_700_000_000_000
    )
}
