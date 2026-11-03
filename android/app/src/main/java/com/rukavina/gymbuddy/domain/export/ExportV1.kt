package com.rukavina.gymbuddy.domain.export

import com.rukavina.gymbuddy.domain.model.ActivityLevel
import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.Equipment
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.ExperienceLevel
import com.rukavina.gymbuddy.domain.model.FitnessGoal
import com.rukavina.gymbuddy.domain.model.Gender
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.domain.model.SetType
import com.rukavina.gymbuddy.domain.model.TrainingDays

/**
 * The export file, formatVersion 1 (ADR 0015). A contract from the first
 * release: fields may only be added. A rename or removal needs
 * formatVersion 2 and an upgrade step in every later importer.
 *
 * Rules: ids are the stored UUIDv7s, timestamps epoch ms, units metric
 * with the unit in the name. Deleted rows (tombstones) are left out.
 * Default exercises and templates are referenced by id only (decision
 * 3A); sessions keep their name snapshots, so history reads without them.
 * Enums are written by name.
 */
data class ExportV1(
    val formatVersion: Int = FORMAT_VERSION,
    val appVersion: String,
    val exportedAt: Long,
    /** "guest" or "account" (ADR 0014). No uid: the file is for the user. */
    val owner: String,
    val profile: ExportProfile?,
    val bodyWeightEntries: List<ExportBodyWeightEntry>,
    val customExercises: List<ExportExercise>,
    val templates: List<ExportTemplate>,
    val sessions: List<ExportSession>,
    val exerciseStates: List<ExportExerciseState>,
    val templateStates: List<ExportTemplateState>
) {
    companion object {
        const val FORMAT_VERSION = 1
        const val OWNER_GUEST = "guest"
        const val OWNER_ACCOUNT = "account"
    }
}

data class ExportProfile(
    val name: String,
    val email: String,
    val birthDate: Long?,
    val weightKg: Float?,
    val heightCm: Float?,
    val targetWeightKg: Float?,
    val gender: Gender?,
    val fitnessGoal: FitnessGoal?,
    val activityLevel: ActivityLevel?,
    val experienceLevel: ExperienceLevel?,
    val trainingDays: TrainingDays?,
    val avatarId: String?,
    val bio: String?,
    val joinedDate: Long,
    val activePlanId: String?,
    val healthDataConsentVersion: String?,
    val healthDataConsentAt: Long?
)

data class ExportBodyWeightEntry(
    val id: String,
    val measuredAt: Long,
    val weightKg: Float
)

data class ExportExercise(
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
    /** The default it was copied from, if any. */
    val derivedFromId: String?
)

data class ExportTemplate(
    val id: String,
    val title: String,
    val derivedFromId: String?,
    val exercises: List<ExportTemplateExercise>
)

data class ExportTemplateExercise(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val trackingType: ExerciseTrackingType,
    val orderIndex: Int,
    val plannedSets: Int,
    val plannedReps: Int?,
    val plannedWeightKg: Float?,
    val plannedDurationSeconds: Int?,
    val plannedDistanceMeters: Float?,
    val restSeconds: Int?,
    val notes: String?
)

data class ExportSession(
    val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val durationSeconds: Int,
    val title: String,
    val notes: String?,
    val templateId: String?,
    val templateTitle: String?,
    val exercises: List<ExportPerformedExercise>
)

data class ExportPerformedExercise(
    val id: String,
    val exerciseId: String,
    val orderIndex: Int,
    val exerciseName: String,
    val category: ExerciseCategory,
    val trackingType: ExerciseTrackingType,
    val primaryMuscles: List<MuscleGroup>,
    val supersetGroup: Int?,
    val sets: List<ExportSet>
)

data class ExportSet(
    val id: String,
    val orderIndex: Int,
    val setType: SetType,
    val isCompleted: Boolean,
    val reps: Int?,
    val weightKg: Float?,
    val durationSeconds: Int?,
    val distanceMeters: Float?,
    val restTakenSeconds: Int?
)

data class ExportExerciseState(
    val exerciseId: String,
    val isHidden: Boolean,
    val isFavorite: Boolean,
    val note: String?,
    val defaultRestSeconds: Int?
)

data class ExportTemplateState(
    val templateId: String,
    val isHidden: Boolean,
    val isFavorite: Boolean
)
