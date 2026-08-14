package com.rukavina.gymbuddy.data.local.mapper

import com.rukavina.gymbuddy.data.local.entity.PerformedExerciseWithSets
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.domain.model.PerformedExercise
import com.rukavina.gymbuddy.domain.model.SetType
import com.rukavina.gymbuddy.domain.model.SyncState
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutSessionMapperTest {

    private fun set(
        id: String,
        orderIndex: Int,
        weightKg: Float? = null,
        reps: Int? = null,
        durationSeconds: Int? = null,
        distanceMeters: Float? = null,
        setType: SetType = SetType.WORKING,
        isCompleted: Boolean = true,
        restTakenSeconds: Int? = null
    ) = WorkoutSet(
        id = id,
        weightKg = weightKg,
        reps = reps,
        durationSeconds = durationSeconds,
        distanceMeters = distanceMeters,
        setType = setType,
        isCompleted = isCompleted,
        restTakenSeconds = restTakenSeconds,
        orderIndex = orderIndex
    )

    private fun performedExercise(
        id: String,
        exerciseId: String,
        orderIndex: Int,
        sets: List<WorkoutSet>,
        supersetGroup: Int? = null
    ) = PerformedExercise(
        id = id,
        exerciseId = exerciseId,
        orderIndex = orderIndex,
        exerciseName = "Exercise $exerciseId",
        exerciseCategory = ExerciseCategory.STRENGTH,
        exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
        exercisePrimaryMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.ARMS),
        sets = sets,
        supersetGroup = supersetGroup
    )

    private fun fullSession(): WorkoutSession {
        val benchSets = listOf(
            set(id = "set-1", orderIndex = 0, weightKg = 60f, reps = 8, setType = SetType.WARMUP),
            set(id = "set-2", orderIndex = 1, weightKg = 100f, reps = 5, setType = SetType.WORKING, restTakenSeconds = 120)
        )
        val plankSets = listOf(
            // Every nullable measurement absent - an in-progress, unfilled row.
            set(id = "set-3", orderIndex = 0, isCompleted = false)
        )

        return WorkoutSession(
            id = "session-1",
            startedAt = 1_700_000_000_000L,
            endedAt = 1_700_003_600_000L,
            durationSeconds = 3600,
            title = "Push Day",
            notes = "felt strong today",
            templateId = "template-1",
            templateTitle = "Push Day Template",
            performedExercises = listOf(
                performedExercise(id = "pe-1", exerciseId = "ex-bench", orderIndex = 0, sets = benchSets, supersetGroup = 1),
                performedExercise(id = "pe-2", exerciseId = "ex-plank", orderIndex = 1, sets = plankSets, supersetGroup = null)
            ),
            updatedAt = 1_700_003_700_000L,
            deletedAt = 1_700_090_000_000L,
            revision = 4,
            syncState = SyncState.CONFLICTED
        )
    }

    /**
     * Rebuilds the PerformedExerciseWithSets list the way
     * WorkoutSessionDao.getPerformedExercisesWithSets would - grouped and
     * already ordered by orderIndex - from the flat entity lists toEntities
     * produces.
     */
    private fun toOrderedRelations(
        performedExerciseEntities: List<com.rukavina.gymbuddy.data.local.entity.PerformedExerciseEntity>,
        setEntities: List<com.rukavina.gymbuddy.data.local.entity.WorkoutSetEntity>
    ): List<PerformedExerciseWithSets> =
        performedExerciseEntities
            .sortedBy { it.orderIndex }
            .map { pe ->
                PerformedExerciseWithSets(
                    performedExercise = pe,
                    sets = setEntities.filter { it.performedExerciseId == pe.id }.sortedBy { it.orderIndex }
                )
            }

    @Test
    fun `round trips a full session through toEntities and toDomain unchanged`() {
        val original = fullSession()

        val (sessionEntity, peEntities, setEntities) = WorkoutSessionMapper.toEntities(original)
        val relations = toOrderedRelations(peEntities, setEntities)
        val result = WorkoutSessionMapper.toDomain(sessionEntity, relations)

        assertEquals(original, result)
    }

    @Test
    fun `toEntities links performed exercises to the session and sets to their performed exercise`() {
        val original = fullSession()

        val (sessionEntity, peEntities, setEntities) = WorkoutSessionMapper.toEntities(original)

        assertEquals(2, peEntities.size)
        peEntities.forEach { assertEquals(sessionEntity.id, it.workoutSessionId) }

        val benchSetEntities = setEntities.filter { it.performedExerciseId == "pe-1" }
        val plankSetEntities = setEntities.filter { it.performedExerciseId == "pe-2" }
        assertEquals(2, benchSetEntities.size)
        assertEquals(1, plankSetEntities.size)
    }

    @Test
    fun `preserves nullable measurements exactly - including all-null incomplete sets`() {
        val original = fullSession()

        val (sessionEntity, peEntities, setEntities) = WorkoutSessionMapper.toEntities(original)
        val result = WorkoutSessionMapper.toDomain(sessionEntity, toOrderedRelations(peEntities, setEntities))

        val plankSet = result.performedExercises.single { it.id == "pe-2" }.sets.single()
        assertNull(plankSet.weightKg)
        assertNull(plankSet.reps)
        assertNull(plankSet.durationSeconds)
        assertNull(plankSet.distanceMeters)
        assertNull(plankSet.restTakenSeconds)
        assertEquals(false, plankSet.isCompleted)
    }

    @Test
    fun `preserves session-level sync metadata and soft-deleted tombstone state`() {
        val original = fullSession()

        val (sessionEntity, peEntities, setEntities) = WorkoutSessionMapper.toEntities(original)
        val result = WorkoutSessionMapper.toDomain(sessionEntity, toOrderedRelations(peEntities, setEntities))

        assertEquals(original.updatedAt, result.updatedAt)
        assertEquals(original.deletedAt, result.deletedAt)
        assertEquals(original.revision, result.revision)
        assertEquals(original.syncState, result.syncState)
    }

    @Test
    fun `preserves supersetGroup including null for exercises not in a superset`() {
        val original = fullSession()

        val (sessionEntity, peEntities, setEntities) = WorkoutSessionMapper.toEntities(original)
        val result = WorkoutSessionMapper.toDomain(sessionEntity, toOrderedRelations(peEntities, setEntities))

        assertEquals(1, result.performedExercises.single { it.id == "pe-1" }.supersetGroup)
        assertNull(result.performedExercises.single { it.id == "pe-2" }.supersetGroup)
    }

    @Test
    fun `toDomain trusts the given relation order instead of re-sorting by orderIndex`() {
        val original = fullSession()
        val (sessionEntity, peEntities, setEntities) = WorkoutSessionMapper.toEntities(original)

        // Deliberately pass performed exercises in reverse of their
        // orderIndex - simulating what would happen if a caller skipped
        // the DAO's ORDER BY. toDomain has no sortedBy of its own for
        // performed exercises, so it must come back out reversed too.
        val outOfOrderRelations = toOrderedRelations(peEntities, setEntities).reversed()

        val result = WorkoutSessionMapper.toDomain(sessionEntity, outOfOrderRelations)

        assertEquals("pe-2", result.performedExercises[0].id)
        assertEquals("pe-1", result.performedExercises[1].id)
    }

    @Test
    fun `performedExerciseToEntities converts a single exercise with an explicit session id`() {
        val exercise = performedExercise(
            id = "pe-standalone",
            exerciseId = "ex-row",
            orderIndex = 2,
            sets = listOf(set(id = "set-standalone", orderIndex = 0, weightKg = 40f, reps = 12))
        )

        val (entity, setEntities) = WorkoutSessionMapper.performedExerciseToEntities(exercise, workoutSessionId = "session-42")

        assertEquals("session-42", entity.workoutSessionId)
        assertEquals("pe-standalone", entity.id)
        assertEquals(1, setEntities.size)
        assertEquals("pe-standalone", setEntities.single().performedExerciseId)
        assertEquals(40f, setEntities.single().weightKg)
    }
}
