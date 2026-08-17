package com.rukavina.gymbuddy.data.local.mapper

import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.Equipment
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.domain.model.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseMapperTest {

    private fun fullExercise() = Exercise(
        id = "ex-1",
        name = "Bench Press",
        primaryMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.ARMS),
        secondaryMuscles = listOf(MuscleGroup.SHOULDERS),
        description = "A compound press",
        instructions = listOf("Lie on bench", "Lower bar", "Press up"),
        tips = listOf("Keep shoulder blades retracted", "Feet flat on the floor"),
        difficulty = DifficultyLevel.INTERMEDIATE,
        equipmentNeeded = listOf(Equipment.BARBELL, Equipment.BENCH),
        category = ExerciseCategory.STRENGTH,
        exerciseType = ExerciseType.COMPOUND,
        trackingType = ExerciseTrackingType.WEIGHT_REPS,
        videoUrl = "https://example.com/video",
        thumbnailUrl = "https://example.com/thumb.jpg",
        source = EntitySource.CUSTOM,
        ownerId = "user-1",
        derivedFromId = "ex-default-1",
        deprecated = true,
        updatedAt = 1_700_000_000_000L,
        deletedAt = 1_700_090_000_000L,
        revision = 3,
        syncState = SyncState.CONFLICTED
    )

    @Test
    fun `round trips a full exercise through toEntity and toDomain unchanged`() {
        val original = fullExercise()

        val result = ExerciseMapper.toDomain(ExerciseMapper.toEntity(original))

        assertEquals(original, result)
    }

    @Test
    fun `round trips an exercise with every nullable field absent`() {
        val original = Exercise(
            id = "ex-2",
            name = "Bodyweight Squat",
            primaryMuscles = listOf(MuscleGroup.LEGS),
            secondaryMuscles = emptyList(),
            description = null,
            instructions = emptyList(),
            tips = emptyList(),
            difficulty = DifficultyLevel.BEGINNER,
            equipmentNeeded = emptyList(),
            category = ExerciseCategory.STRENGTH,
            exerciseType = ExerciseType.COMPOUND,
            trackingType = ExerciseTrackingType.REPS_ONLY,
            videoUrl = null,
            thumbnailUrl = null,
            ownerId = null,
            derivedFromId = null
        )

        val result = ExerciseMapper.toDomain(ExerciseMapper.toEntity(original))

        assertEquals(original, result)
        assertNull(result.description)
        assertNull(result.videoUrl)
        assertNull(result.deletedAt)
    }

    @Test
    fun `preserves deprecation and sync metadata across the round trip`() {
        val original = fullExercise()

        val result = ExerciseMapper.toDomain(ExerciseMapper.toEntity(original))

        assertEquals(true, result.deprecated)
        assertEquals(original.updatedAt, result.updatedAt)
        assertEquals(original.deletedAt, result.deletedAt)
        assertEquals(original.revision, result.revision)
        assertEquals(original.syncState, result.syncState)
    }

    @Test
    fun `toDomainList and toEntityList map every element`() {
        val originals = listOf(fullExercise(), fullExercise().copy(id = "ex-3", name = "Squat"))

        val roundTripped = ExerciseMapper.toDomainList(ExerciseMapper.toEntityList(originals))

        assertEquals(originals, roundTripped)
    }
}
