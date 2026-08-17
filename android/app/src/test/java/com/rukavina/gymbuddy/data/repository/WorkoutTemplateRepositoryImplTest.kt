package com.rukavina.gymbuddy.data.repository

import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.TemplateExercise
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkoutTemplateRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: WorkoutTemplateRepositoryImpl
    private val fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(org.robolectric.RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutTemplateRepositoryImpl(db.workoutTemplateDao(), db.userTemplateStateDao(), fixedClock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun templateExercise(id: String) = TemplateExercise(
        id = id,
        exerciseId = "ex-1",
        exerciseName = "Bench Press",
        exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
        plannedSets = 3,
        plannedReps = 8,
        orderIndex = 0
    )

    private fun template(id: String, title: String = "Push Day") = WorkoutTemplate(
        id = id,
        title = title,
        templateExercises = listOf(templateExercise("te-$id"))
    )

    @Test
    fun `createTemplate stamps updatedAt from the injected Clock`() = runBlocking {
        repository.createTemplate(template("template-1"))

        val read = repository.getTemplateById("template-1")!!
        assertEquals(fixedClock.millis(), read.updatedAt)
    }

    @Test
    fun `createTemplate persists a template that reads back with its exercises`() = runBlocking {
        repository.createTemplate(template("template-1"))

        val read = repository.getTemplateById("template-1")!!
        assertEquals("Push Day", read.title)
        assertEquals(1, read.templateExercises.size)
        assertEquals("Bench Press", read.templateExercises.single().exerciseName)
    }

    @Test
    fun `deleteTemplate tombstones instead of removing the row`() = runBlocking {
        repository.createTemplate(template("template-1"))

        repository.deleteTemplate("template-1")

        assertNull(repository.getTemplateById("template-1"))
        db.openHelper.readableDatabase.query(
            "SELECT deletedAt FROM workout_templates WHERE id = 'template-1'"
        ).use { cursor ->
            assertTrue("the row must still exist after a tombstone delete", cursor.moveToFirst())
            assertEquals(fixedClock.millis(), cursor.getLong(0))
        }
    }

    @Test
    fun `getAllTemplates excludes deleted templates but keeps other live templates`() = runBlocking {
        repository.createTemplate(template("template-1"))
        repository.createTemplate(template("template-2"))

        repository.deleteTemplate("template-1")

        val all = repository.getAllTemplates().first()
        assertEquals(listOf("template-2"), all.map { it.id })
    }

    @Test
    fun `hideTemplate excludes it from getAllTemplates and unhideTemplate restores it`() = runBlocking {
        repository.createTemplate(template("template-1"))

        repository.hideTemplate("template-1")
        assertTrue(repository.getAllTemplates().first().none { it.id == "template-1" })
        assertEquals(listOf("template-1"), repository.getHiddenTemplates().first().map { it.id })

        repository.unhideTemplate("template-1")
        assertEquals(listOf("template-1"), repository.getAllTemplates().first().map { it.id })
        assertTrue(repository.getHiddenTemplates().first().isEmpty())
    }

    @Test
    fun `updateTemplate replaces exercises and re-stamps updatedAt`() = runBlocking {
        repository.createTemplate(template("template-1"))

        val laterClock = Clock.fixed(fixedClock.instant().plusSeconds(3600), ZoneOffset.UTC)
        val laterRepository = WorkoutTemplateRepositoryImpl(db.workoutTemplateDao(), db.userTemplateStateDao(), laterClock)
        val updated = template("template-1", title = "Push Day (edited)")
        laterRepository.updateTemplate(updated)

        val read = repository.getTemplateById("template-1")!!
        assertEquals("Push Day (edited)", read.title)
        assertEquals(laterClock.millis(), read.updatedAt)
    }

    @Test(expected = IllegalStateException::class)
    fun `createTemplate refuses to persist a template exercise with an unstamped snapshot`() = runBlocking {
        val unstamped = template("template-1").let {
            it.copy(templateExercises = it.templateExercises.map { te -> te.copy(exerciseName = "") })
        }

        repository.createTemplate(unstamped)
        Unit
    }
}
