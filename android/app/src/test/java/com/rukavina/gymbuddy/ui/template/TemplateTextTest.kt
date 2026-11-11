package com.rukavina.gymbuddy.ui.template

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.TemplateExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

/** Template text helpers: relative day, target line, about-minutes. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TemplateTextTest {

    @get:Rule
    val rule = createComposeRule()

    /** setContent runs once per test, so all texts of a test render together. */
    private fun texts(vararg blocks: @Composable () -> String?): List<String?> {
        val out = arrayOfNulls<String>(blocks.size)
        rule.setContent { blocks.forEachIndexed { i, b -> out[i] = b() } }
        rule.waitForIdle()
        return out.toList()
    }

    private val today = LocalDate.of(2026, 11, 11)
    private fun daysAgo(n: Long) = today.minusDays(n).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun relativeDay_buckets() {
        val t = texts(
            { relativeDay(daysAgo(0), today, ZoneOffset.UTC) },
            { relativeDay(daysAgo(1), today, ZoneOffset.UTC) },
            { relativeDay(daysAgo(3), today, ZoneOffset.UTC) },
            { relativeDay(daysAgo(30), today, ZoneOffset.UTC) },
        )
        assertEquals(listOf("today", "yesterday", "3 days ago"), t.take(3))
        assertTrue(t[3], !t[3]!!.contains("ago"))
    }

    @Test
    fun aboutText_nullWhenUnknown() {
        val t = texts({ aboutText(null) }, { aboutText(45) })
        assertNull(t[0])
        assertEquals("about 45 min", t[1])
    }

    private fun ex(type: ExerciseTrackingType, reps: Int? = null, secs: Int? = null, meters: Float? = null, kg: Float? = null) =
        TemplateExercise(
            id = "t", exerciseId = "e", exerciseName = "E", exerciseTrackingType = type, plannedSets = 3,
            plannedReps = reps, plannedDurationSeconds = secs, plannedDistanceMeters = meters, plannedWeightKg = kg, orderIndex = 0,
        )

    @Test
    fun targetText_perTrackingType() {
        val m = PreferredUnits.METRIC
        val t = texts(
            { targetText(ex(ExerciseTrackingType.WEIGHT_REPS, reps = 6, kg = 80f), m) },
            { targetText(ex(ExerciseTrackingType.DURATION, secs = 60), m) },
            { targetText(ex(ExerciseTrackingType.DISTANCE_DURATION, meters = 400f), m) },
            { targetText(ex(ExerciseTrackingType.REPS_ONLY, kg = 20f), m) },
        )
        assertEquals(listOf("3 × 6 · 80 kg", "3 × 1:00", "3 × 400 m", "3 sets"), t)
    }
}
