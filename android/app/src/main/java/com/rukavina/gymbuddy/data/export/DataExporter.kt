package com.rukavina.gymbuddy.data.export

import androidx.room.withTransaction
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.domain.export.ExportBodyWeightEntry
import com.rukavina.gymbuddy.domain.export.ExportExercise
import com.rukavina.gymbuddy.domain.export.ExportExerciseState
import com.rukavina.gymbuddy.domain.export.ExportPerformedExercise
import com.rukavina.gymbuddy.domain.export.ExportProfile
import com.rukavina.gymbuddy.domain.export.ExportSession
import com.rukavina.gymbuddy.domain.export.ExportSet
import com.rukavina.gymbuddy.domain.export.ExportTemplate
import com.rukavina.gymbuddy.domain.export.ExportTemplateExercise
import com.rukavina.gymbuddy.domain.export.ExportTemplateState
import com.rukavina.gymbuddy.domain.export.ExportV1
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.sync.withLock
import okio.buffer
import okio.sink
import java.io.OutputStream
import java.time.Clock

/**
 * Writes the open owner's data as an ExportV1 JSON file (ADR 0015).
 *
 * The snapshot is read under the holder's lock (no owner switch or sync
 * cycle runs meanwhile) in one transaction, from the open file only, so
 * no other owner's rows can get in (ADR 0014). Works offline and for
 * guests.
 */
class DataExporter(
    private val holder: DatabaseHolder,
    private val appVersion: String,
    private val clock: Clock
) {

    /** Reads the snapshot. */
    suspend fun snapshot(): ExportV1 = holder.lock.withLock {
        val owner = holder.owner
        val db = holder.db
        db.withTransaction { read(db, owner) }
    }

    /** Reads the snapshot and writes it to [out] as UTF-8 JSON. Doesn't close [out]. */
    suspend fun export(out: OutputStream) {
        val data = snapshot()
        val sink = out.sink().buffer()
        ADAPTER.toJson(sink, data)
        sink.writeUtf8("\n")
        sink.flush()
    }

    private suspend fun read(db: AppDatabase, owner: String?): ExportV1 {
        val dao = db.exportDao()
        val profileUid = owner ?: UserProfileRepository.GUEST_UID
        val profile = db.userProfileDao().getUserProfile(profileUid)?.let {
            ExportProfile(
                name = it.name,
                email = it.email,
                birthDate = it.birthDate,
                weightKg = it.weight,
                heightCm = it.height,
                targetWeightKg = it.targetWeight,
                gender = it.gender,
                fitnessGoal = it.fitnessGoal,
                activityLevel = it.activityLevel,
                experienceLevel = it.experienceLevel,
                trainingDays = it.trainingDays,
                avatarId = it.avatarId,
                bio = it.bio,
                joinedDate = it.joinedDate,
                activePlanId = it.activePlanId,
                healthDataConsentVersion = it.healthDataConsentVersion,
                healthDataConsentAt = it.healthDataConsentAt
            )
        }

        val templateExercises = dao.customTemplateExercises().groupBy { it.templateId }
        val setsByExercise = dao.sets().groupBy { it.performedExerciseId }
        val performedBySession = dao.performedExercises().groupBy { it.workoutSessionId }

        return ExportV1(
            appVersion = appVersion,
            exportedAt = clock.millis(),
            owner = if (owner == null) ExportV1.OWNER_GUEST else ExportV1.OWNER_ACCOUNT,
            profile = profile,
            bodyWeightEntries = dao.bodyWeightEntries().map {
                ExportBodyWeightEntry(it.id, it.measuredAt, it.weightKg)
            },
            customExercises = dao.customExercises().map {
                ExportExercise(
                    id = it.id,
                    name = it.name,
                    primaryMuscles = it.primaryMuscles,
                    secondaryMuscles = it.secondaryMuscles,
                    description = it.description,
                    instructions = it.instructions,
                    tips = it.tips,
                    difficulty = it.difficulty,
                    equipmentNeeded = it.equipmentNeeded,
                    category = it.category,
                    exerciseType = it.exerciseType,
                    trackingType = it.trackingType,
                    derivedFromId = it.derivedFromId
                )
            },
            templates = dao.customTemplates().map { t ->
                ExportTemplate(
                    id = t.id,
                    title = t.title,
                    derivedFromId = t.derivedFromId,
                    exercises = templateExercises[t.id].orEmpty().map {
                        ExportTemplateExercise(
                            id = it.id,
                            exerciseId = it.exerciseId,
                            exerciseName = it.exerciseName,
                            trackingType = it.exerciseTrackingType,
                            orderIndex = it.orderIndex,
                            plannedSets = it.plannedSets,
                            plannedReps = it.plannedReps,
                            plannedWeightKg = it.plannedWeightKg,
                            plannedDurationSeconds = it.plannedDurationSeconds,
                            plannedDistanceMeters = it.plannedDistanceMeters,
                            restSeconds = it.restSeconds,
                            notes = it.notes
                        )
                    }
                )
            },
            sessions = dao.sessions().map { s ->
                ExportSession(
                    id = s.id,
                    startedAt = s.startedAt,
                    endedAt = s.endedAt,
                    durationSeconds = s.durationSeconds,
                    title = s.title,
                    notes = s.notes,
                    templateId = s.templateId,
                    templateTitle = s.templateTitle,
                    exercises = performedBySession[s.id].orEmpty().map { pe ->
                        ExportPerformedExercise(
                            id = pe.id,
                            exerciseId = pe.exerciseId,
                            orderIndex = pe.orderIndex,
                            exerciseName = pe.exerciseName,
                            category = pe.exerciseCategory,
                            trackingType = pe.exerciseTrackingType,
                            primaryMuscles = pe.exercisePrimaryMuscles,
                            supersetGroup = pe.supersetGroup,
                            sets = setsByExercise[pe.id].orEmpty().map {
                                ExportSet(
                                    id = it.id,
                                    orderIndex = it.orderIndex,
                                    setType = it.setType,
                                    isCompleted = it.isCompleted,
                                    reps = it.reps,
                                    weightKg = it.weightKg,
                                    durationSeconds = it.durationSeconds,
                                    distanceMeters = it.distanceMeters,
                                    restTakenSeconds = it.restTakenSeconds
                                )
                            }
                        )
                    }
                )
            },
            exerciseStates = dao.exerciseStates().map {
                ExportExerciseState(it.exerciseId, it.isHidden, it.isFavorite, it.note, it.defaultRestSeconds)
            },
            templateStates = dao.templateStates().map {
                ExportTemplateState(it.templateId, it.isHidden, it.isFavorite)
            }
        )
    }

    companion object {
        /** Nulls are written, so every v1 field is always present. */
        private val ADAPTER = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
            .adapter(ExportV1::class.java)
            .serializeNulls()
            .indent("  ")

        /** `gymbuddy-export-2026-11-03.json`, in the device's zone. */
        fun fileName(clock: Clock): String =
            "gymbuddy-export-${java.time.LocalDate.now(clock)}.json"
    }
}
