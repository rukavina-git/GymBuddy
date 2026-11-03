package com.rukavina.gymbuddy.testutil

import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.Equipment
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.MuscleGroup

/** Exercise builder for editor / picker tests (Plan 7 05b). */
object ExerciseFixtures {
    fun exercise(
        id: String,
        name: String = id,
        muscle: MuscleGroup = MuscleGroup.CHEST,
        equipment: Equipment = Equipment.BARBELL,
        trackingType: ExerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
    ) = Exercise(
        id = id,
        name = name,
        primaryMuscles = listOf(muscle),
        secondaryMuscles = emptyList(),
        difficulty = DifficultyLevel.INTERMEDIATE,
        equipmentNeeded = listOf(equipment),
        category = ExerciseCategory.STRENGTH,
        exerciseType = ExerciseType.COMPOUND,
        trackingType = trackingType,
    )
}
