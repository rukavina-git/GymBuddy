package com.rukavina.gymbuddy.data.local.seeder

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Plan 10 step 05b: what the bundled starter plans contain. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StarterPlanContentTest {

    private val exercises = JSONObject(File("src/main/assets/default_exercises.json").readText())
        .getJSONArray("exercises").let { a -> (0 until a.length()).map { a.getJSONObject(it) } }
        .associateBy { it.getString("id") }
    private val bundle = JSONObject(File("src/main/assets/default_workout_templates.json").readText())
    private val templates = bundle.getJSONArray("templates").let { a -> (0 until a.length()).map { a.getJSONObject(it) } }
        .associateBy { it.getString("id") }
    private val plans = bundle.getJSONArray("plans").let { a -> (0 until a.length()).map { a.getJSONObject(it) } }

    private fun rows(templateId: String) = templates.getValue(templateId).getJSONArray("exercises")
        .let { a -> (0 until a.length()).map { a.getJSONObject(it) } }

    private fun planRows(title: String) = plans.single { it.getString("title") == title }
        .getJSONArray("templateIds").let { a -> (0 until a.length()).flatMap { rows(a.getString(it)) } }

    @Test
    fun planRowsMatchLiveExercises() {
        plans.flatMap { p -> p.getJSONArray("templateIds").let { a -> (0 until a.length()).map { a.getString(it) } } }
            .flatMap(::rows)
            .forEach { r ->
                val e = exercises.getValue(r.getString("exerciseId"))
                assertFalse(e.getString("name"), e.getBoolean("deprecated"))
                assertEquals(e.getString("name"), r.getString("exerciseName"))
                assertEquals(e.getString("name"), e.getString("trackingType"), r.getString("exerciseTrackingType"))
            }
    }

    @Test
    fun fullBodyIsBeginnerFriendlyWithTheCoachesWeeklySets() {
        val rows = planRows("Full Body")
        rows.forEach { r ->
            val e = exercises.getValue(r.getString("exerciseId"))
            assertTrue(e.getString("name"), e.getString("difficulty") != "ADVANCED")
        }
        fun sets(muscle: String) = rows.filter {
            exercises.getValue(it.getString("exerciseId")).getJSONArray("primaryMuscles").toString().contains(muscle)
        }.sumOf { it.getInt("plannedSets") }
        assertEquals(6, sets("CHEST"))
        assertEquals(9, sets("BACK"))
    }

    @Test
    fun pullUpIsNoLongerAPlanDefault() {
        val names = plans.flatMap { p -> p.getJSONArray("templateIds").let { a -> (0 until a.length()).map { a.getString(it) } } }
            .flatMap(::rows).map { it.getString("exerciseName") }
        assertFalse("Pull-Up" in names)
        assertFalse("Hanging Leg Raise" in names)
        assertTrue("Lat Pulldown" in names)
    }
}
