package com.rukavina.gymbuddy.ui.progress

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.WorkoutSet
import com.rukavina.gymbuddy.domain.progress.Lift
import com.rukavina.gymbuddy.domain.progress.LiftPoint
import com.rukavina.gymbuddy.domain.progress.LiftProgress
import com.rukavina.gymbuddy.testutil.ProgressFixtures.day
import com.rukavina.gymbuddy.testutil.ProgressFixtures.lift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Locale

/** Progress text helpers (step 08): sentences, ranges, inline sets, chart labels. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ProgressTextTest {

    @get:Rule
    val rule = createComposeRule()

    private fun progress(vararg kg: Float, weeks: Int = 8) = LiftProgress(
        lift = Lift("bench", "Bench press", kg.size),
        points = kg.mapIndexed { i, w -> LiftPoint("s$i", i * 1000L, WorkoutSet(orderIndex = 0, id = "w$i", weightKg = w, reps = 5)) },
        weeks = weeks,
    )

    /** Runs a composable text function once and returns its result. */
    private fun text(block: @androidx.compose.runtime.Composable () -> String): String {
        var out = ""
        rule.setContent { out = block() }
        rule.waitForIdle()
        return out
    }

    @Test
    fun liftSentence_more() {
        assertEquals("You lift 7.5 kg more than 8 weeks ago.", text { liftSentence(progress(80f, 87.5f), PreferredUnits.METRIC) })
    }

    @Test
    fun liftSentence_less() {
        assertTrue(text { liftSentence(progress(90f, 85f, weeks = 1), PreferredUnits.METRIC) }.contains("5 kg"))
    }

    @Test
    fun liftSentence_same() {
        assertEquals("You lift the same as 1 week ago.", text { liftSentence(progress(80f, 80f, weeks = 1), PreferredUnits.METRIC) })
    }

    @Test
    fun heaviestRange_andChartSummary() {
        val p = progress(80f, 87.5f)
        assertEquals("Heaviest set each session · 80 → 87.5 kg", text { heaviestRange(p, PreferredUnits.METRIC) })
    }

    @Test
    fun chartSummary_namesLift() {
        val s = text { chartSummary(progress(80f, 87.5f), PreferredUnits.METRIC) }
        assertTrue(s, s.contains("Bench press chart"))
    }

    @Test
    fun sessionSubtitle_timeAndTopSet() {
        val pe = lift("squat", 110f to 5, name = "Squat")
        val row = SessionRow(day("a", 1, pe, minutes = 64), isPr = false, top = pe to pe.sets.first())
        val s = text { sessionSubtitle(row, PreferredUnits.METRIC) }
        assertTrue(s, s.contains("64") && s.contains("Squat"))
    }

    @Test
    fun sessionSubtitle_noTimeNoTop() {
        val row = SessionRow(day("a", 1, minutes = 0), isPr = false, top = null)
        assertEquals("", text { sessionSubtitle(row, PreferredUnits.METRIC) })
    }

    @Test
    fun inlineSets_weightRepsUnitOnce() {
        val sets = listOf(
            WorkoutSet(orderIndex = 0, id = "1", weightKg = 87.5f, reps = 5),
            WorkoutSet(orderIndex = 0, id = "2", weightKg = 85f),
            WorkoutSet(orderIndex = 0, id = "3", reps = 6),
            WorkoutSet(orderIndex = 0, id = "4", durationSeconds = 90),
            WorkoutSet(orderIndex = 0, id = "5", distanceMeters = 400f),
            WorkoutSet(orderIndex = 0, id = "6"),
        )
        assertEquals(
            "Bench 87.5 × 5 · 85 · 6 · 1:30 · 400 m kg",
            text { inlineSets("Bench", sets, PreferredUnits.METRIC) },
        )
    }

    @Test
    fun inlineSets_noWeightNoUnit() {
        assertEquals("Plank 1:00", text { inlineSets("Plank", listOf(WorkoutSet(orderIndex = 0, id = "1", durationSeconds = 60)), PreferredUnits.METRIC) })
    }

    @Test
    fun dates_inLocale() {
        // Day 256 after the epoch is 14 Sep 1970.
        val d = dayMonth(256 * 86_400_000L, ZoneOffset.UTC, Locale.UK)
        assertTrue(d, d.contains("14") && d.contains("Sep"))
        assertEquals("October 2026", monthTitle(YearMonth.of(2026, 10), Locale.US))
    }

    @Test
    fun tickText_halfOnlyWhenNeeded() {
        assertEquals("80", tickText(80f))
        assertTrue(tickText(82.5f).startsWith("82"))
    }

    @Test
    fun chartTicks_coverRange() {
        val ticks = chartTicks(progress(80f, 100f), PreferredUnits.METRIC)
        assertTrue(ticks.first() <= 80f && ticks.last() >= 100f)
    }
}
