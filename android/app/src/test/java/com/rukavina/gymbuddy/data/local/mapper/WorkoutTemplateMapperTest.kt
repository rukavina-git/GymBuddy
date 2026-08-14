package com.rukavina.gymbuddy.data.local.mapper

import com.rukavina.gymbuddy.data.local.entity.WorkoutTemplateWithExercises
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.SyncState
import com.rukavina.gymbuddy.domain.model.TemplateExercise
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutTemplateMapperTest {

    private fun templateExercise(
        id: String,
        exerciseId: String,
        orderIndex: Int,
        trackingType: ExerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
        plannedSets: Int = 3,
        plannedReps: Int? = 10,
        restSeconds: Int? = 90,
        plannedDurationSeconds: Int? = null,
        plannedDistanceMeters: Float? = null,
        plannedWeightKg: Float? = null,
        notes: String? = null
    ) = TemplateExercise(
        id = id,
        exerciseId = exerciseId,
        exerciseName = "Exercise $exerciseId",
        exerciseTrackingType = trackingType,
        plannedSets = plannedSets,
        plannedReps = plannedReps,
        orderIndex = orderIndex,
        restSeconds = restSeconds,
        plannedDurationSeconds = plannedDurationSeconds,
        plannedDistanceMeters = plannedDistanceMeters,
        plannedWeightKg = plannedWeightKg,
        notes = notes
    )

    private fun fullTemplate(): WorkoutTemplate = WorkoutTemplate(
        id = "template-1",
        title = "Push Day",
        templateExercises = listOf(
            templateExercise(
                id = "te-1",
                exerciseId = "ex-bench",
                orderIndex = 0,
                trackingType = ExerciseTrackingType.WEIGHT_REPS,
                plannedSets = 4,
                plannedReps = 8,
                restSeconds = 120,
                plannedWeightKg = 60f,
                notes = "controlled eccentric"
            ),
            templateExercise(
                id = "te-2",
                exerciseId = "ex-plank",
                orderIndex = 1,
                trackingType = ExerciseTrackingType.DURATION,
                plannedSets = 3,
                plannedReps = null,
                restSeconds = null,
                plannedDurationSeconds = 60,
                notes = null
            )
        ),
        source = EntitySource.CUSTOM,
        ownerId = "user-1",
        derivedFromId = "template-default-1",
        deprecated = true,
        updatedAt = 1_700_000_000_000L,
        deletedAt = 1_700_090_000_000L,
        revision = 2,
        syncState = SyncState.PENDING
    )

    @Test
    fun `round trips a full template through toEntities and toDomain unchanged`() {
        val original = fullTemplate()

        val (templateEntity, exerciseEntities) = WorkoutTemplateMapper.toEntities(original)
        val result = WorkoutTemplateMapper.toDomain(WorkoutTemplateWithExercises(templateEntity, exerciseEntities))

        assertEquals(original, result)
    }

    @Test
    fun `toEntities links every template exercise to the template id`() {
        val original = fullTemplate()

        val (templateEntity, exerciseEntities) = WorkoutTemplateMapper.toEntities(original)

        assertEquals(2, exerciseEntities.size)
        exerciseEntities.forEach { assertEquals(templateEntity.id, it.templateId) }
    }

    @Test
    fun `preserves nullable planned measurements exactly per exercise`() {
        val original = fullTemplate()

        val (templateEntity, exerciseEntities) = WorkoutTemplateMapper.toEntities(original)
        val result = WorkoutTemplateMapper.toDomain(WorkoutTemplateWithExercises(templateEntity, exerciseEntities))

        val bench = result.templateExercises.single { it.id == "te-1" }
        assertEquals(8, bench.plannedReps)
        assertEquals(120, bench.restSeconds)
        assertEquals(60f, bench.plannedWeightKg)
        assertNull(bench.plannedDurationSeconds)
        assertNull(bench.plannedDistanceMeters)

        val plank = result.templateExercises.single { it.id == "te-2" }
        assertNull(plank.plannedReps)
        assertNull(plank.restSeconds)
        assertNull(plank.plannedWeightKg)
        assertEquals(60, plank.plannedDurationSeconds)
        assertNull(plank.notes)
    }

    @Test
    fun `preserves template-level sync metadata, source and deprecation`() {
        val original = fullTemplate()

        val (templateEntity, exerciseEntities) = WorkoutTemplateMapper.toEntities(original)
        val result = WorkoutTemplateMapper.toDomain(WorkoutTemplateWithExercises(templateEntity, exerciseEntities))

        assertEquals(original.source, result.source)
        assertEquals(original.ownerId, result.ownerId)
        assertEquals(original.derivedFromId, result.derivedFromId)
        assertEquals(original.deprecated, result.deprecated)
        assertEquals(original.updatedAt, result.updatedAt)
        assertEquals(original.deletedAt, result.deletedAt)
        assertEquals(original.revision, result.revision)
        assertEquals(original.syncState, result.syncState)
    }

    @Test
    fun `toDomain sorts template exercises by orderIndex regardless of relation order`() {
        val original = fullTemplate()
        val (templateEntity, exerciseEntities) = WorkoutTemplateMapper.toEntities(original)

        // Unlike WorkoutSessionMapper.toDomain, this mapper explicitly
        // sorts - so a scrambled relation list must still come back in
        // orderIndex order.
        val scrambled = exerciseEntities.reversed()
        assertEquals("te-2", scrambled.first().id)

        val result = WorkoutTemplateMapper.toDomain(WorkoutTemplateWithExercises(templateEntity, scrambled))

        assertEquals("te-1", result.templateExercises[0].id)
        assertEquals("te-2", result.templateExercises[1].id)
    }

    @Test
    fun `toDomainList maps every template independently`() {
        val first = fullTemplate()
        val second = fullTemplate().copy(
            id = "template-2",
            title = "Pull Day",
            templateExercises = listOf(templateExercise(id = "te-3", exerciseId = "ex-row", orderIndex = 0))
        )

        val relations = listOf(first, second).map { template ->
            val (entity, exercises) = WorkoutTemplateMapper.toEntities(template)
            WorkoutTemplateWithExercises(entity, exercises)
        }

        val result = WorkoutTemplateMapper.toDomainList(relations)

        assertEquals(listOf("template-1", "template-2"), result.map { it.id })
        assertEquals(first, result[0])
        assertEquals(second, result[1])
    }

    @Test
    fun `templateExerciseToEntity converts a single exercise with an explicit template id`() {
        val exercise = templateExercise(id = "te-standalone", exerciseId = "ex-row", orderIndex = 5, plannedWeightKg = 30f)

        val entity = WorkoutTemplateMapper.templateExerciseToEntity(exercise, templateId = "template-99")

        assertEquals("template-99", entity.templateId)
        assertEquals("te-standalone", entity.id)
        assertEquals(30f, entity.plannedWeightKg)
        assertEquals(5, entity.orderIndex)
    }
}
