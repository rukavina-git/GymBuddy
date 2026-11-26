package com.rukavina.gymbuddy.domain.search

import com.rukavina.gymbuddy.testutil.ExerciseFixtures.exercise
import com.rukavina.gymbuddy.ui.exercises.filterExercises
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseAliasesTest {

    private val bundle = JSONObject(File("src/main/assets/default_exercises.json").readText())
        .getJSONArray("exercises")
        .let { arr -> (0 until arr.length()).map { arr.getJSONObject(it) } }

    private val library = bundle
        .filter { !it.getBoolean("deprecated") }
        .map { exercise(it.getString("id"), it.getString("name")) }

    private fun search(q: String) = filterExercises(library, q, null).map { it.name }

    @Test
    fun everyAliasBelongsToALiveBuiltInExercise() {
        val live = bundle.filter { !it.getBoolean("deprecated") }.map { it.getString("id") }.toSet()
        val unknown = ExerciseAliases.ids - live
        assertTrue("aliases for unknown or deprecated ids: $unknown", unknown.isEmpty())
    }

    @Test
    fun aliasesFindTheExercise() {
        assertTrue(search("glute bridge").containsAll(listOf("Glute Bridge", "Hip Thrust")))
        assertTrue("Tricep Pushdown" in search("rope"))
        assertTrue("Cable Crossover" in search("cable fly"))
    }

    @Test
    fun nameMatchStillComesFirst() {
        assertEquals("Glute Bridge", search("glute bridge").first())
    }

    @Test
    fun noAliasMeansNameOnly() {
        assertEquals(listOf("Treadmill"), search("treadmill"))
    }
}
