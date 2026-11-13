package com.rukavina.gymbuddy.data.sync

import com.rukavina.gymbuddy.data.local.entity.ExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.PerformedExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.PerformedExerciseWithSets
import com.rukavina.gymbuddy.data.local.entity.TemplateExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.UserExerciseStateEntity
import com.rukavina.gymbuddy.data.local.entity.UserProfileEntity
import com.rukavina.gymbuddy.data.local.entity.UserTemplateStateEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutSessionEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutSetEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutTemplateEntity
import com.rukavina.gymbuddy.data.remote.generated.infrastructure.Serializer
import com.rukavina.gymbuddy.data.remote.generated.models.PullResponse
import com.rukavina.gymbuddy.domain.model.ActivityLevel
import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.Equipment
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.FitnessGoal
import com.rukavina.gymbuddy.domain.model.Gender
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.domain.model.SetType
import com.rukavina.gymbuddy.domain.model.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.UUID

/**
 * Scenario 1: UUID mapper round-trip. Every synced type goes Room entity
 * -> generated DTO -> JSON (the generated Moshi instance the app uses)
 * -> DTO -> Room entity, and must come back identical. Covered twice
 * per type: all optional fields populated (including a tombstone
 * deletedAt), and all optional fields null.
 */
class SyncDtoMapperTest {

    private fun id() = UUID.randomUUID().toString()

    /** Serialise through JSON so UUID/URI adapters are exercised, not just the in-memory mapping. */
    private fun viaJson(response: PullResponse): PullResponse {
        val adapter = Serializer.moshi.adapter(PullResponse::class.java)
        return adapter.fromJson(adapter.toJson(response))!!
    }

    private fun wrap(
        sessions: List<com.rukavina.gymbuddy.data.remote.generated.models.WorkoutSession>? = null,
        exercises: List<com.rukavina.gymbuddy.data.remote.generated.models.Exercise>? = null,
        templates: List<com.rukavina.gymbuddy.data.remote.generated.models.WorkoutTemplate>? = null,
        exerciseStates: List<com.rukavina.gymbuddy.data.remote.generated.models.UserExerciseState>? = null,
        templateStates: List<com.rukavina.gymbuddy.data.remote.generated.models.UserTemplateState>? = null,
        profile: com.rukavina.gymbuddy.data.remote.generated.models.UserProfile? = null,
    ) = PullResponse(
        nextCursor = "c",
        hasMore = false,
        workoutSessions = sessions,
        exercises = exercises,
        workoutTemplates = templates,
        userExerciseStates = exerciseStates,
        userTemplateStates = templateStates,
        userProfile = profile
    )

    // ---- Exercise ----

    private fun exercise(full: Boolean) = ExerciseEntity(
        id = id(),
        name = "Custom Curl",
        primaryMuscles = listOf(MuscleGroup.ARMS),
        secondaryMuscles = if (full) listOf(MuscleGroup.SHOULDERS) else emptyList(),
        description = "desc".takeIf { full },
        instructions = if (full) listOf("step 1", "step 2") else emptyList(),
        tips = if (full) listOf("tip") else emptyList(),
        difficulty = DifficultyLevel.INTERMEDIATE,
        equipmentNeeded = listOf(Equipment.DUMBBELL),
        category = ExerciseCategory.STRENGTH,
        exerciseType = ExerciseType.ISOLATION,
        trackingType = ExerciseTrackingType.WEIGHT_DURATION,
        videoUrl = "https://example.com/v.mp4".takeIf { full },
        thumbnailUrl = "https://example.com/t.png".takeIf { full },
        source = EntitySource.CUSTOM,
        ownerId = "uid-1".takeIf { full },
        derivedFromId = id().takeIf { full },
        deprecated = full,
        updatedAt = 123L,
        deletedAt = 456L.takeIf { full },
        revision = 7,
        syncState = SyncState.SYNCED
    )

    @Test
    fun `exercise round-trips with every optional field set, including deletedAt`() {
        val original = exercise(full = true)
        val back = viaJson(wrap(exercises = listOf(SyncDtoMapper.toDto(original)))).exercises!!.single()
        assertEquals(original, SyncDtoMapper.toEntity(back))
    }

    @Test
    fun `exercise round-trips with every optional field null`() {
        val original = exercise(full = false)
        val back = viaJson(wrap(exercises = listOf(SyncDtoMapper.toDto(original)))).exercises!!.single()
        assertEquals(original, SyncDtoMapper.toEntity(back))
    }

    // ---- WorkoutSession aggregate ----

    private fun sessionAggregate(full: Boolean): SessionAggregate {
        val sessionId = id()
        val performed = (0..1).map { i ->
            PerformedExerciseEntity(
                id = id(),
                workoutSessionId = sessionId,
                exerciseId = id(),
                orderIndex = i,
                exerciseName = "Exercise $i",
                exerciseCategory = ExerciseCategory.CARDIO,
                exerciseTrackingType = ExerciseTrackingType.DISTANCE_DURATION,
                exercisePrimaryMuscles = listOf(MuscleGroup.LEGS, MuscleGroup.CORE),
                supersetGroup = 1.takeIf { full }
            )
        }
        val sets = performed.flatMap { pe ->
            (0..1).map { i ->
                WorkoutSetEntity(
                    id = id(),
                    performedExerciseId = pe.id,
                    weightKg = 42.5f.takeIf { full },
                    reps = 10.takeIf { full },
                    durationSeconds = 300.takeIf { full },
                    distanceMeters = 1000.5f.takeIf { full },
                    setType = if (i == 0) SetType.WARMUP else SetType.WORKING,
                    isCompleted = full,
                    restTakenSeconds = 60.takeIf { full },
                    orderIndex = i
                )
            }
        }
        val session = WorkoutSessionEntity(
            id = sessionId,
            startedAt = 1_000L,
            endedAt = 2_000L.takeIf { full },
            durationSeconds = 1,
            title = "Session",
            notes = "notes".takeIf { full },
            templateId = id().takeIf { full },
            templateTitle = "Template".takeIf { full },
            updatedAt = 99L,
            deletedAt = 3_000L.takeIf { full },
            revision = 3,
            syncState = SyncState.SYNCED
        )
        return SessionAggregate(session, performed, sets)
    }

    private fun roundTrip(aggregate: SessionAggregate): SessionAggregate {
        val withSets = aggregate.performedExercises.map { pe ->
            PerformedExerciseWithSets(pe, aggregate.sets.filter { it.performedExerciseId == pe.id })
        }
        val dto = SyncDtoMapper.toDto(aggregate.session, withSets)
        return SyncDtoMapper.toEntities(viaJson(wrap(sessions = listOf(dto))).workoutSessions!!.single())
    }

    @Test
    fun `workout session aggregate round-trips with every optional field set, including deletedAt`() {
        val original = sessionAggregate(full = true)
        assertEquals(original, roundTrip(original))
    }

    @Test
    fun `workout session aggregate round-trips with every optional field null`() {
        val original = sessionAggregate(full = false)
        assertEquals(original, roundTrip(original))
    }

    // ---- WorkoutTemplate aggregate ----

    private fun templateAggregate(full: Boolean): TemplateAggregate {
        val templateId = id()
        return TemplateAggregate(
            template = WorkoutTemplateEntity(
                id = templateId,
                title = "Template",
                source = EntitySource.CUSTOM,
                ownerId = "uid-1".takeIf { full },
                derivedFromId = id().takeIf { full },
                deprecated = full,
                updatedAt = 5L,
                deletedAt = 6L.takeIf { full },
                revision = 2,
                syncState = SyncState.SYNCED
            ),
            templateExercises = (0..2).map { i ->
                TemplateExerciseEntity(
                    id = id(),
                    templateId = templateId,
                    exerciseId = id(),
                    exerciseName = "Exercise $i",
                    exerciseTrackingType = ExerciseTrackingType.WEIGHT_DISTANCE,
                    plannedSets = 3,
                    plannedReps = 8.takeIf { full },
                    orderIndex = i,
                    restSeconds = 90.takeIf { full },
                    plannedDurationSeconds = 60.takeIf { full },
                    plannedDistanceMeters = 500f.takeIf { full },
                    plannedWeightKg = 20f.takeIf { full },
                    notes = "note".takeIf { full }
                )
            }
        )
    }

    @Test
    fun `workout template aggregate round-trips with optional fields set and null`() {
        listOf(true, false).forEach { full ->
            val original = templateAggregate(full)
            val dto = SyncDtoMapper.toDto(original.template, original.templateExercises)
            val back = SyncDtoMapper.toEntities(viaJson(wrap(templates = listOf(dto))).workoutTemplates!!.single())
            assertEquals("full=$full", original, back)
        }
    }

    // ---- Overlay rows ----

    @Test
    fun `overlay rows round-trip with optional fields set and null`() {
        listOf(true, false).forEach { full ->
            val exerciseState = UserExerciseStateEntity(
                exerciseId = id(),
                isHidden = full,
                isFavorite = !full,
                note = "note".takeIf { full },
                defaultRestSeconds = 120.takeIf { full },
                updatedAt = 1L,
                revision = 4,
                syncState = SyncState.SYNCED
            )
            val templateState = UserTemplateStateEntity(
                templateId = id(),
                isHidden = full,
                isFavorite = !full,
                updatedAt = 2L,
                revision = 5,
                syncState = SyncState.SYNCED
            )
            val back = viaJson(
                wrap(
                    exerciseStates = listOf(SyncDtoMapper.toDto(exerciseState)),
                    templateStates = listOf(SyncDtoMapper.toDto(templateState))
                )
            )
            assertEquals(exerciseState, SyncDtoMapper.toEntity(back.userExerciseStates!!.single()))
            assertEquals(templateState, SyncDtoMapper.toEntity(back.userTemplateStates!!.single()))
        }
    }

    // ---- Profile ----

    @Test
    fun `profile round-trips with optional fields set, including deletedAt, and null`() {
        listOf(true, false).forEach { full ->
            val original = UserProfileEntity(
                uid = "firebase-uid-not-a-uuid",
                name = "Name",
                email = "a@b.c",
                profileImageUrl = "https://example.com/p.jpg".takeIf { full },
                birthDate = 10L.takeIf { full },
                weight = 80f.takeIf { full },
                height = 180f.takeIf { full },
                gender = Gender.FEMALE.takeIf { full },
                fitnessGoal = FitnessGoal.BUILD_MUSCLE.takeIf { full },
                activityLevel = ActivityLevel.VERY_ACTIVE.takeIf { full },
                targetWeight = 75f.takeIf { full },
                joinedDate = 1L,
                bio = "bio".takeIf { full },
                updatedAt = 2L,
                deletedAt = 3L.takeIf { full },
                revision = 1,
                syncState = SyncState.SYNCED
            )
            val back = viaJson(wrap(profile = SyncDtoMapper.toDto(original))).userProfile!!
            assertEquals("full=$full", original, SyncDtoMapper.toEntity(back))
        }
    }

    // ---- Failure modes ----

    @Test
    fun `a non-UUID local id cannot be mapped to a DTO`() {
        assertThrows(IllegalArgumentException::class.java) {
            SyncDtoMapper.toDto(exercise(full = false).copy(id = "not-a-uuid"))
        }
    }

    @Test
    fun `ids survive the UUID conversion with their exact string form`() {
        val original = exercise(full = true)
        val dto = SyncDtoMapper.toDto(original)
        assertEquals(original.id, dto.id.toString())
        assertEquals(original.derivedFromId, dto.derivedFromId.toString())
    }
}
