package com.rukavina.gymbuddy.ui.exercises.create

import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.Equipment
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.MuscleGroup

/**
 * What Create A / Create C edit (Figma R13-4, R13-6). Only what changes
 * logging is up front; the rest is under More details. Weight + reps is
 * the default (android/CLAUDE.md: the tracking picker is an override).
 * Difficulty, category and kind are hidden data fields with defaults
 * (step decision 5): Beginner, from what you log, Compound.
 */
data class ExerciseForm(
    val name: String = "",
    val tracking: ExerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
    /** Null = none picked: saved as Other. */
    val mainMuscle: MuscleGroup? = null,
    val equipment: Set<Equipment> = emptySet(),
    val otherMuscles: Set<MuscleGroup> = emptySet(),
    /** How-to steps, one per line. */
    val steps: String = "",
    /** Your rest for it; null = the default. */
    val restSeconds: Int? = null,
    val difficulty: DifficultyLevel = DifficultyLevel.BEGINNER,
    val kind: ExerciseType = ExerciseType.COMPOUND,
) {
    val canSave: Boolean get() = name.isNotBlank()

    /** The exercise to write: [base] = the one being edited (keeps its id, owner and other fields). */
    fun toExercise(id: String, base: Exercise? = null): Exercise {
        val main = mainMuscle ?: MuscleGroup.OTHER
        val fields = (base ?: Exercise(
            id = id,
            name = "",
            primaryMuscles = emptyList(),
            secondaryMuscles = emptyList(),
            difficulty = DifficultyLevel.BEGINNER,
            equipmentNeeded = emptyList(),
            category = ExerciseCategory.STRENGTH,
            exerciseType = ExerciseType.COMPOUND,
            trackingType = ExerciseTrackingType.WEIGHT_REPS,
            source = EntitySource.CUSTOM,
        ))
        return fields.copy(
            name = name.trim(),
            trackingType = tracking,
            primaryMuscles = listOf(main),
            secondaryMuscles = otherMuscles.filter { it != main }.sortedBy { it.ordinal },
            equipmentNeeded = equipment.sortedBy { it.ordinal },
            instructions = steps.lines().map { it.trim() }.filter { it.isNotEmpty() },
            difficulty = difficulty,
            exerciseType = kind,
            category = categoryFor(tracking, base?.category),
        )
    }

    companion object {
        /** Distance + time reads as cardio; otherwise strength (an edited exercise keeps its own). */
        fun categoryFor(tracking: ExerciseTrackingType, current: ExerciseCategory?): ExerciseCategory = when {
            current != null -> current
            tracking == ExerciseTrackingType.DISTANCE_DURATION -> ExerciseCategory.CARDIO
            else -> ExerciseCategory.STRENGTH
        }

        fun of(exercise: Exercise, restSeconds: Int?) = ExerciseForm(
            name = exercise.name,
            tracking = exercise.trackingType,
            mainMuscle = exercise.primaryMuscles.firstOrNull(),
            equipment = exercise.equipmentNeeded.toSet(),
            otherMuscles = exercise.secondaryMuscles.toSet(),
            steps = exercise.instructions.joinToString("\n"),
            restSeconds = restSeconds,
            difficulty = exercise.difficulty,
            kind = exercise.exerciseType,
        )
    }
}

/** First tracking chips; the rest sit behind "More" (CLAUDE.md: not six equal choices). */
val MainTracking = listOf(ExerciseTrackingType.WEIGHT_REPS, ExerciseTrackingType.REPS_ONLY, ExerciseTrackingType.DURATION)

/** Equipment chips before "More", in Figma order (R13-4). */
val MainEquipment = listOf(
    Equipment.BODYWEIGHT, Equipment.BARBELL, Equipment.DUMBBELL, Equipment.CABLE,
    Equipment.MACHINE, Equipment.KETTLEBELL, Equipment.RESISTANCE_BAND,
)

/** Muscle chips on create (with Other, R13-4). */
val CreateMuscles = MuscleGroup.entries.toList()
