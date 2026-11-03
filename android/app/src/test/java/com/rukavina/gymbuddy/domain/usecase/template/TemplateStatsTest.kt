package com.rukavina.gymbuddy.domain.usecase.template

import com.rukavina.gymbuddy.testutil.TemplateFixtures.DAY
import com.rukavina.gymbuddy.testutil.TemplateFixtures.plan
import com.rukavina.gymbuddy.testutil.TemplateFixtures.session
import com.rukavina.gymbuddy.testutil.TemplateFixtures.template
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class TemplateStatsTest {

    // 2026-01-01 00:00 UTC, a Thursday.
    private val thu = 1_767_225_600_000L

    @Test
    fun lastDoneTakesNewestAndSkipsTombstonesAndFreeSessions() {
        val done = TemplateStats.lastDone(
            listOf(
                session("s1", "a", thu - 3 * DAY),
                session("s2", "a", thu - DAY),
                session("s3", "a", thu, deletedAt = thu),
                session("s4", null, thu),
            )
        )
        assertEquals(mapOf("a" to thu - DAY), done)
    }

    @Test
    fun lastDoneFallsBackToStartWithoutEnd() {
        val s = session("s", "a", endedAt = null).copy(startedAt = 42L)
        assertEquals(42L, TemplateStats.lastDone(listOf(s))["a"])
    }

    @Test
    fun averageMinutesRoundsToFiveAndNeedsAFinishedSession() {
        assertNull(TemplateStats.averageMinutes("a", emptyList()))
        assertNull(TemplateStats.averageMinutes("a", listOf(session("s", "a", endedAt = null))))
        val sessions = listOf(
            session("s1", "a", thu, durationSeconds = 41 * 60),
            session("s2", "a", thu, durationSeconds = 47 * 60),
            session("s3", "b", thu, durationSeconds = 90 * 60),
        )
        assertEquals(45, TemplateStats.averageMinutes("a", sessions))
        assertEquals(5, TemplateStats.averageMinutes("a", listOf(session("s", "a", thu, durationSeconds = 60))))
    }

    @Test
    fun homeShowsThreeMostRecentlyDone() {
        val templates = listOf("a", "b", "c", "d").map { template(it) }
        val sessions = listOf(
            session("1", "a", thu - 4 * DAY),
            session("2", "b", thu - DAY),
            session("3", "c", thu - 2 * DAY),
            session("4", "d", thu - 3 * DAY),
        )
        val rows = TemplateStats.homeTemplates(templates, sessions, activePlan = plan("p", "a"))
        assertEquals(listOf("b", "c", "d"), rows.map { it.id })
    }

    @Test
    fun newUserGetsActivePlanTemplatesInPlanOrder() {
        val templates = listOf("a", "b", "c", "d").map { template(it) }
        val rows = TemplateStats.homeTemplates(templates, emptyList(), plan("p", "c", "a", "gone", "b", "d"))
        assertEquals(listOf("c", "a", "b"), rows.map { it.id })
        assertEquals(emptyList<String>(), TemplateStats.homeTemplates(templates, emptyList(), null))
    }

    @Test
    fun byRecencyPutsNeverDoneLastByTitle() {
        val list = listOf(template("x", "Zed"), template("y", "alpha"), template("z", "Mid"))
        val sorted = TemplateStats.byRecency(list, mapOf("z" to 5L))
        assertEquals(listOf("z", "y", "x"), sorted.map { it.id })
    }

    @Test
    fun weekIsMondayFirstAndCountsOnlyThisWeek() {
        val today = LocalDate.of(2026, 1, 1)
        val week = TemplateStats.week(
            listOf(
                session("mon", "a", thu - 3 * DAY + 1, durationSeconds = 3600),
                session("thu", "a", thu + 1, durationSeconds = 1800),
                session("sun-before", "a", thu - 4 * DAY + 1),
                session("gone", "a", thu + 1, deletedAt = thu),
            ),
            today,
            ZoneOffset.UTC,
        )
        assertEquals(LocalDate.of(2025, 12, 29), week.monday)
        assertEquals(setOf(LocalDate.of(2025, 12, 29), today), week.doneDays)
        assertEquals(2, week.sessionCount)
        assertEquals(5400L, week.totalSeconds)
    }

    @Test
    fun planDaySkipsDeprecatedPlans() {
        val plans = listOf(plan("old", "a", "b", deprecated = true), plan("new", "c", "b"))
        val (p, day) = TemplateStats.planDay("b", plans)!!
        assertEquals("new", p.id)
        assertEquals(2, day)
        assertNull(TemplateStats.planDay("a", plans))
    }

    @Test
    fun copyTitleTakesFirstFreeName() {
        val name = { n: Int -> if (n == 1) "Push (copy)" else "Push (copy $n)" }
        assertEquals("Push (copy)", TemplateStats.copyTitle(listOf("Push"), name))
        assertEquals("Push (copy 3)", TemplateStats.copyTitle(listOf("Push (copy)", "Push (copy 2)"), name))
    }
}
