package com.rukavina.gymbuddy.ui.exercises.page

import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseImagesTest {

    @Test
    fun everyImageBelongsToABuiltInExercise() {
        val json = JSONObject(File("src/main/assets/default_exercises.json").readText())
        val exercises = json.getJSONArray("exercises")
        val ids = (0 until exercises.length()).map { exercises.getJSONObject(it).getString("id") }.toSet()
        val unknown = ExerciseImages.ids - ids
        assertTrue("images for unknown exercise ids: $unknown", unknown.isEmpty())
    }

    @Test
    fun lookupIgnoresCase() {
        val id = ExerciseImages.ids.first()
        assertTrue(ExerciseImages.forId(id.uppercase()) != null)
    }
}
