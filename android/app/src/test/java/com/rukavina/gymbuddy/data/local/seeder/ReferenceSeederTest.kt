package com.rukavina.gymbuddy.data.local.seeder

import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.entity.ExerciseVersionEntity
import com.rukavina.gymbuddy.data.local.entity.TemplateVersionEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutTemplateEntity
import com.rukavina.gymbuddy.domain.model.EntitySource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertTrue(ExerciseSeeder(db.exerciseDao(), db.exerciseVersionDao()).seedIfNeeded(context))
        assertTrue(WorkoutTemplateSeeder(db.workoutTemplateDao(), db.templateVersionDao()).seedIfNeeded(context))

        assertTrue(db.exerciseDao().getDefaultExerciseCount() > 0)
        assertTrue(db.workoutTemplateDao().getDefaultTemplates().first().isNotEmpty())
        assertEquals(bundledVersion("default_exercises.json"), db.exerciseVersionDao().getCurrentVersion()!!.version)
        assertEquals(bundledVersion("default_workout_templates.json"), db.templateVersionDao().getCurrentVersion()!!.version)
    }

    @Test
    fun `a server-provided exercise library older than the bundle is not overwritten on launch`() = runBlocking {
        val serverVersion = bundledVersion("default_exercises.json") - 1
        db.exerciseVersionDao().setVersion(ExerciseVersionEntity(version = serverVersion))

        val seeded = ExerciseSeeder(db.exerciseDao(), db.exerciseVersionDao()).seedIfNeeded(context)

        assertFalse(seeded)
        assertEquals(0, db.exerciseDao().getDefaultExerciseCount())
        assertEquals(serverVersion, db.exerciseVersionDao().getCurrentVersion()!!.version)
    }

    @Test
    fun `a server-provided template library older than the bundle is not overwritten on launch`() = runBlocking {
        val serverVersion = bundledVersion("default_workout_templates.json") - 1
        db.templateVersionDao().setVersion(TemplateVersionEntity(version = serverVersion))
        db.workoutTemplateDao().insertTemplate(WorkoutTemplateEntity(id = "server-template", title = "From server", source = EntitySource.DEFAULT))

        val seeded = WorkoutTemplateSeeder(db.workoutTemplateDao(), db.templateVersionDao()).seedIfNeeded(context)

        assertFalse(seeded)
        assertEquals(listOf("server-template"), db.workoutTemplateDao().getDefaultTemplates().first().map { it.template.id })
        assertEquals(serverVersion, db.templateVersionDao().getCurrentVersion()!!.version)
    }
}
