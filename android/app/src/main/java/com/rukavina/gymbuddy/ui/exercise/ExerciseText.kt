package com.rukavina.gymbuddy.ui.exercise

import androidx.annotation.StringRes
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.Equipment
import com.rukavina.gymbuddy.domain.model.MuscleGroup

/** Display names for exercise enums (picker rows and chips; 7.7 reuses them). */

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
