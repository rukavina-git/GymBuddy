package com.rukavina.gymbuddy.ui.exercise

import androidx.annotation.StringRes
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.Equipment
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.MuscleGroup

/** Display names for exercise enums (picker, exercise page, create). */

@StringRes
fun muscleLabel(muscle: MuscleGroup): Int = when (muscle) {
    MuscleGroup.CHEST -> R.string.muscle_chest
    MuscleGroup.BACK -> R.string.muscle_back
    MuscleGroup.LEGS -> R.string.muscle_legs
    MuscleGroup.SHOULDERS -> R.string.muscle_shoulders
    MuscleGroup.ARMS -> R.string.muscle_arms
    MuscleGroup.CORE -> R.string.muscle_core
    MuscleGroup.OTHER -> R.string.muscle_other
}

@StringRes
fun equipmentLabel(equipment: Equipment): Int = when (equipment) {
    Equipment.BARBELL -> R.string.equipment_barbell
    Equipment.DUMBBELL -> R.string.equipment_dumbbell
    Equipment.KETTLEBELL -> R.string.equipment_kettlebell
    Equipment.CABLE -> R.string.equipment_cable
    Equipment.MACHINE -> R.string.equipment_machine
    Equipment.BODYWEIGHT -> R.string.equipment_bodyweight
    Equipment.RESISTANCE_BAND -> R.string.equipment_resistance_band
    Equipment.EZ_BAR -> R.string.equipment_ez_bar
    Equipment.TRAP_BAR -> R.string.equipment_trap_bar
    Equipment.PULL_UP_BAR -> R.string.equipment_pull_up_bar
    Equipment.BENCH -> R.string.equipment_bench
    Equipment.MEDICINE_BALL -> R.string.equipment_medicine_ball
    Equipment.SUSPENSION_TRAINER -> R.string.equipment_suspension_trainer
    Equipment.SMITH_MACHINE -> R.string.equipment_smith_machine
}

@StringRes
fun trackingLabel(type: ExerciseTrackingType): Int = when (type) {
    ExerciseTrackingType.WEIGHT_REPS -> R.string.tracking_weight_reps
    ExerciseTrackingType.REPS_ONLY -> R.string.tracking_reps_only
    ExerciseTrackingType.DURATION -> R.string.tracking_duration
    ExerciseTrackingType.WEIGHT_DURATION -> R.string.tracking_weight_duration
    ExerciseTrackingType.DISTANCE_DURATION -> R.string.tracking_distance_duration
    ExerciseTrackingType.WEIGHT_DISTANCE -> R.string.tracking_weight_distance
}

@StringRes
fun difficultyLabel(level: DifficultyLevel): Int = when (level) {
    DifficultyLevel.BEGINNER -> R.string.difficulty_beginner
    DifficultyLevel.INTERMEDIATE -> R.string.difficulty_intermediate
    DifficultyLevel.ADVANCED -> R.string.difficulty_advanced
}

@StringRes
fun kindLabel(type: ExerciseType): Int = when (type) {
    ExerciseType.COMPOUND -> R.string.kind_compound
    ExerciseType.ISOLATION -> R.string.kind_isolation
}
