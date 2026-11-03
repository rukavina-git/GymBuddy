package com.rukavina.gymbuddy.data.export

import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.local.entity.BodyWeightEntryEntity
import com.rukavina.gymbuddy.data.local.entity.ExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.PerformedExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.TemplateExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.UserExerciseStateEntity
import com.rukavina.gymbuddy.data.local.entity.UserProfileEntity
import com.rukavina.gymbuddy.data.local.entity.UserTemplateStateEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutSessionEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutSetEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutTemplateEntity
import com.rukavina.gymbuddy.domain.export.ExportV1
import com.rukavina.gymbuddy.domain.model.ActivityLevel
import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.EntitySource
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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Plan 6 step 07. The v1 fixture (src/test/resources/export/v1-sample.json)
 * is the format contract (ADR 0015): later importers must read it, so it
 * only changes by adding fields. On a mismatch the actual output lands in
 * build/export-actual.json.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataExporterTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-03T10:00:00Z"), ZoneOffset.UTC)
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun exporter(owner: String? = UID) = DataExporter(DatabaseHolder.fixed(db, owner), "1.0.0-test", clock)

    private fun exportJson(owner: String? = UID): String =
        ByteArrayOutputStream().also { runBlocking { exporter(owner).export(it) } }.toString(Charsets.UTF_8)

    @Test
    fun `seeded data exports exactly as the committed v1 fixture`() {
        runBlocking { seed() }
        val actual = exportJson()
        val expected = javaClass.getResource("/export/v1-sample.json")?.readText()
        if (expected != actual) File("build/export-actual.json").writeText(actual)
        assertEquals(expected, actual)
    }

    @Test
    fun `tombstones and defaults are left out, overlays on defaults kept`() = runBlocking {
        seed()
        val data = exporter().snapshot()
        assertEquals(listOf(EX_CUSTOM), data.customExercises.map { it.id })
        assertEquals(listOf(TPL_CUSTOM), data.templates.map { it.id })
        assertEquals(listOf(SESSION), data.sessions.map { it.id })
        assertEquals(listOf(BW_LIVE), data.bodyWeightEntries.map { it.id })
        assertTrue(data.exerciseStates.any { it.exerciseId == EX_DEFAULT && it.isHidden })
        assertEquals(ExportV1.OWNER_ACCOUNT, data.owner)
        assertEquals(1, data.formatVersion)
    }

    @Test
    fun `only the open owner's profile is exported`() = runBlocking {
        db.userProfileDao().insertUserProfile(profile(uid = "someone-else"))
        assertNull(exporter().snapshot().profile)

        db.userProfileDao().insertUserProfile(profile(uid = "guest", name = "Guest me"))
        val guest = exporter(owner = null).snapshot()
        assertEquals(ExportV1.OWNER_GUEST, guest.owner)
        assertEquals("Guest me", guest.profile?.name)
    }

    @Test
    fun `empty guest file exports valid JSON with empty arrays`() {
        val json = exportJson(owner = null)
        assertTrue(json.contains("\"formatVersion\": 1"))
        assertTrue(json.contains("\"profile\": null"))
        assertTrue(json.contains("\"sessions\": []"))
        assertTrue(json.contains("\"bodyWeightEntries\": []"))
        assertFalse(json.contains("uid"))
    }

    @Test
    fun `file name uses the date`() {
        assertEquals("gymbuddy-export-2026-11-03.json", DataExporter.fileName(clock))
    }

    private suspend fun seed() {
        db.userProfileDao().insertUserProfile(profile())
        db.bodyWeightDao().upsert(BodyWeightEntryEntity(id = BW_LIVE, measuredAt = 1_790_000_000_000, weightKg = 80.5f))
        db.bodyWeightDao().upsert(
            BodyWeightEntryEntity(id = BW_DELETED, measuredAt = 1_790_000_100_000, weightKg = 81f, deletedAt = 1_790_000_200_000)
        )
        db.exerciseDao().insertExercises(
            listOf(
                exercise(EX_CUSTOM, "Cable \"Fly\", low", EntitySource.CUSTOM),
                exercise(EX_DEFAULT, "Bench Press", EntitySource.DEFAULT),
                exercise(EX_DELETED, "Old one", EntitySource.CUSTOM, deletedAt = 1_790_000_000_000)
            )
        )
        db.workoutTemplateDao().insertTemplate(WorkoutTemplateEntity(id = TPL_CUSTOM, title = "My Push", source = EntitySource.CUSTOM, ownerId = UID))
        db.workoutTemplateDao().insertTemplate(WorkoutTemplateEntity(id = TPL_DEFAULT, title = "Push", source = EntitySource.DEFAULT))
        db.workoutTemplateDao().insertTemplateExercises(
            listOf(
                templateExercise("019a0000-0000-7000-8000-000000000301", TPL_CUSTOM, EX_DEFAULT, "Bench Press", 0),
                templateExercise("019a0000-0000-7000-8000-000000000302", TPL_DEFAULT, EX_DEFAULT, "Bench Press", 0)
            )
        )
        db.workoutSessionDao().insertWorkoutSession(
            WorkoutSessionEntity(
                id = SESSION, startedAt = 1_790_001_000_000, endedAt = 1_790_004_600_000, durationSeconds = 3600,
                title = "My Push", notes = "Felt good, line\nbreak", templateId = TPL_CUSTOM, templateTitle = "My Push"
            )
        )
        db.workoutSessionDao().insertWorkoutSession(
            WorkoutSessionEntity(
                id = SESSION_DELETED, startedAt = 1_790_002_000_000, durationSeconds = 60, title = "Gone",
                deletedAt = 1_790_003_000_000
            )
        )
        db.workoutSessionDao().insertPerformedExercises(
            listOf(
                performed(PE, SESSION),
                performed("019a0000-0000-7000-8000-000000000402", SESSION_DELETED)
            )
        )
        db.workoutSessionDao().insertWorkoutSets(
            listOf(
                WorkoutSetEntity(id = "019a0000-0000-7000-8000-000000000501", performedExerciseId = PE, weightKg = 60f, reps = 8,
                    setType = SetType.WARMUP, isCompleted = true, restTakenSeconds = 90, orderIndex = 0),
                WorkoutSetEntity(id = "019a0000-0000-7000-8000-000000000502", performedExerciseId = PE, weightKg = 82.5f, reps = 5,
                    isCompleted = true, orderIndex = 1),
                WorkoutSetEntity(id = "019a0000-0000-7000-8000-000000000503", performedExerciseId = "019a0000-0000-7000-8000-000000000402",
                    weightKg = 10f, reps = 1, orderIndex = 0)
            )
        )
        db.userExerciseStateDao().upsert(UserExerciseStateEntity(exerciseId = EX_DEFAULT, isHidden = true, note = "Elbows in", defaultRestSeconds = 120))
        db.userTemplateStateDao().upsert(UserTemplateStateEntity(templateId = TPL_DEFAULT, isFavorite = true))
    }

    private fun profile(uid: String = UID, name: String = "Ana") = UserProfileEntity(
        uid = uid, name = name, email = "ana@example.com", birthDate = 631_152_000_000, weight = 80.5f, height = 172f,
        gender = Gender.FEMALE, fitnessGoal = FitnessGoal.values().first(), activityLevel = ActivityLevel.values().first(),
        targetWeight = 75f, joinedDate = 1_780_000_000_000, experienceLevel = ExperienceLevel.values().first(),
        avatarId = "cat", trainingDays = TrainingDays.values().first(), healthDataConsentVersion = "1",
        healthDataConsentAt = 1_780_000_000_000, activePlanId = "019a0000-0000-7000-8000-000000000901"
    )

    private fun exercise(id: String, name: String, source: EntitySource, deletedAt: Long? = null) = ExerciseEntity(
        id = id, name = name, primaryMuscles = listOf(MuscleGroup.values().first()), secondaryMuscles = emptyList(),
        description = null, instructions = listOf("Step one"), tips = emptyList(), difficulty = DifficultyLevel.values().first(),
        equipmentNeeded = listOf(Equipment.values().first()), category = ExerciseCategory.values().first(),
        exerciseType = ExerciseType.values().first(), trackingType = ExerciseTrackingType.WEIGHT_REPS,
        videoUrl = null, thumbnailUrl = null, source = source, ownerId = if (source == EntitySource.CUSTOM) UID else null,
        derivedFromId = null, deletedAt = deletedAt
    )

    private fun templateExercise(id: String, templateId: String, exerciseId: String, name: String, order: Int) = TemplateExerciseEntity(
        id = id, templateId = templateId, exerciseId = exerciseId, exerciseName = name,
        exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS, plannedSets = 3, plannedReps = 8, orderIndex = order,
        restSeconds = 120, plannedWeightKg = 80f, notes = null
    )

    private fun performed(id: String, sessionId: String) = PerformedExerciseEntity(
        id = id, workoutSessionId = sessionId, exerciseId = EX_DEFAULT, orderIndex = 0, exerciseName = "Bench Press",
        exerciseCategory = ExerciseCategory.values().first(), exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
        exercisePrimaryMuscles = listOf(MuscleGroup.values().first())
    )

    private companion object {
        const val UID = "account-uid"
        const val BW_LIVE = "019a0000-0000-7000-8000-000000000001"
        const val BW_DELETED = "019a0000-0000-7000-8000-000000000002"
        const val EX_CUSTOM = "019a0000-0000-7000-8000-000000000101"
        const val EX_DEFAULT = "019a0000-0000-7000-8000-000000000102"
        const val EX_DELETED = "019a0000-0000-7000-8000-000000000103"
        const val TPL_CUSTOM = "019a0000-0000-7000-8000-000000000201"
        const val TPL_DEFAULT = "019a0000-0000-7000-8000-000000000202"
        const val SESSION = "019a0000-0000-7000-8000-000000000601"
        const val SESSION_DELETED = "019a0000-0000-7000-8000-000000000602"
        const val PE = "019a0000-0000-7000-8000-000000000401"
    }
}
