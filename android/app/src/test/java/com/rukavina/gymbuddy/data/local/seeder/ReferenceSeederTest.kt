package com.rukavina.gymbuddy.data.local.seeder

import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.entity.ExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.ExerciseVersionEntity
import com.rukavina.gymbuddy.data.local.entity.TemplateVersionEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutTemplateEntity
import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.ExerciseType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * D-35: the bundled reference library seeds a first install only. Once
 * any library version is stored - in particular one the server returned
 * - the seeder must leave it alone, even when the app's bundle is newer.
 * Otherwise every cold start reseeds the bundle and the next sync
 * replaces it again.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReferenceSeederTest {

    private lateinit var db: AppDatabase
    private val context = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun bundledVersion(asset: String) =
        JSONObject(context.assets.open(asset).bufferedReader().use { it.readText() }).getInt("version")

    @Test
    fun `a first install seeds the bundled library and records its version`() = runBlocking {
        assertTrue(ExerciseSeeder(DatabaseHolder.fixed(db)).seedIfNeeded(context))
        assertTrue(WorkoutTemplateSeeder(DatabaseHolder.fixed(db)).seedIfNeeded(context))

        assertTrue(db.exerciseDao().getDefaultExerciseCount() > 0)
        assertTrue(db.workoutTemplateDao().getDefaultTemplates().first().isNotEmpty())
        assertEquals(bundledVersion("default_exercises.json"), db.exerciseVersionDao().getCurrentVersion()!!.version)
        assertEquals(bundledVersion("default_workout_templates.json"), db.templateVersionDao().getCurrentVersion()!!.version)
    }

    @Test
    fun `a server-provided exercise library older than the bundle is not overwritten on launch`() = runBlocking {
        val serverVersion = bundledVersion("default_exercises.json") - 1
        db.exerciseVersionDao().setVersion(ExerciseVersionEntity(version = serverVersion))

        val seeded = ExerciseSeeder(DatabaseHolder.fixed(db)).seedIfNeeded(context)

        assertFalse(seeded)
        assertEquals(0, db.exerciseDao().getDefaultExerciseCount())
        assertEquals(serverVersion, db.exerciseVersionDao().getCurrentVersion()!!.version)
    }

    @Test
    fun `a server-provided template library older than the bundle is not overwritten on launch`() = runBlocking {
        val serverVersion = bundledVersion("default_workout_templates.json") - 1
        db.templateVersionDao().setVersion(TemplateVersionEntity(version = serverVersion))
        db.workoutTemplateDao().insertTemplate(WorkoutTemplateEntity(id = "server-template", title = "From server", source = EntitySource.DEFAULT))

        val seeded = WorkoutTemplateSeeder(DatabaseHolder.fixed(db)).seedIfNeeded(context)

        assertFalse(seeded)
        assertEquals(listOf("server-template"), db.workoutTemplateDao().getDefaultTemplates().first().map { it.template.id })
        assertEquals(serverVersion, db.templateVersionDao().getCurrentVersion()!!.version)
    }

    // ---- guest file (null owner): never syncs, so the bundle is its source ----

    private fun guest() = DatabaseHolder.fixed(db, owner = null)

    private fun exercise(id: String, source: EntitySource, ownerId: String?) = ExerciseEntity(
        id = id, name = id, primaryMuscles = emptyList(), secondaryMuscles = emptyList(),
        description = null, instructions = emptyList(), tips = emptyList(),
        difficulty = DifficultyLevel.BEGINNER, equipmentNeeded = emptyList(),
        category = ExerciseCategory.entries.first(), exerciseType = ExerciseType.entries.first(),
        trackingType = ExerciseTrackingType.WEIGHT_REPS, videoUrl = null, thumbnailUrl = null,
        source = source, ownerId = ownerId, derivedFromId = null
    )

    @Test
    fun `a guest exercise library older than the bundle is refreshed, custom exercises kept`() = runBlocking {
        db.exerciseVersionDao().setVersion(ExerciseVersionEntity(version = bundledVersion("default_exercises.json") - 1))
        db.exerciseDao().insertExercises(
            listOf(
                exercise(id = "old-default", source = EntitySource.DEFAULT, ownerId = null),
                exercise(id = "mine", source = EntitySource.CUSTOM, ownerId = "guest")
            )
        )

        assertTrue(ExerciseSeeder(guest()).seedIfNeeded(context))

        assertEquals(null, db.exerciseDao().getExerciseById("old-default"))
        assertNotNull(db.exerciseDao().getExerciseById("mine"))
        assertTrue(db.exerciseDao().getDefaultExerciseCount() > 0)
        assertEquals(bundledVersion("default_exercises.json"), db.exerciseVersionDao().getCurrentVersion()!!.version)
    }

    @Test
    fun `a guest template library older than the bundle is refreshed with plans, custom templates kept`() = runBlocking {
        db.templateVersionDao().setVersion(TemplateVersionEntity(version = bundledVersion("default_workout_templates.json") - 1))
        db.workoutTemplateDao().insertTemplate(WorkoutTemplateEntity(id = "old-default", title = "Old", source = EntitySource.DEFAULT))
        db.workoutTemplateDao().insertTemplate(WorkoutTemplateEntity(id = "mine", title = "Mine", source = EntitySource.CUSTOM, ownerId = "guest"))

        assertTrue(WorkoutTemplateSeeder(guest()).seedIfNeeded(context))

        val defaults = db.workoutTemplateDao().getDefaultTemplates().first().map { it.template.id }
        assertFalse("old-default" in defaults)
        assertTrue(defaults.isNotEmpty())
        assertNotNull(db.workoutTemplateDao().getTemplateById("mine"))
        assertTrue(db.starterPlanDao().getPlans().isNotEmpty())
        assertEquals(bundledVersion("default_workout_templates.json"), db.templateVersionDao().getCurrentVersion()!!.version)
    }

    @Test
    fun `a guest library already at the bundle version is left alone`() = runBlocking {
        db.exerciseVersionDao().setVersion(ExerciseVersionEntity(version = bundledVersion("default_exercises.json")))
        db.templateVersionDao().setVersion(TemplateVersionEntity(version = bundledVersion("default_workout_templates.json")))

        assertFalse(ExerciseSeeder(guest()).seedIfNeeded(context))
        assertFalse(WorkoutTemplateSeeder(guest()).seedIfNeeded(context))
        assertEquals(0, db.exerciseDao().getDefaultExerciseCount())
    }

    @Test
    fun `seeding waits for the switch lock, so the owner can't change mid-seed`() = runBlocking {
        val holder = guest()
        holder.lock.lock()
        val seeding = async(Dispatchers.Default) { ExerciseSeeder(holder).seedIfNeeded(context) }
        delay(200)
        assertFalse(seeding.isCompleted)
        assertEquals(0, db.exerciseDao().getDefaultExerciseCount())

        holder.lock.unlock()
        assertTrue(seeding.await())
    }
}
