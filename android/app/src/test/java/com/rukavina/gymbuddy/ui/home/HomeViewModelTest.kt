package com.rukavina.gymbuddy.ui.home

import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.testutil.FakeTemplateSources
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import com.rukavina.gymbuddy.testutil.MutableClock
import com.rukavina.gymbuddy.testutil.TemplateFixtures.DAY
import com.rukavina.gymbuddy.testutil.TemplateFixtures.plan
import com.rukavina.gymbuddy.testutil.TemplateFixtures.session
import com.rukavina.gymbuddy.testutil.TemplateFixtures.template
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class HomeViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val clock = MutableClock() // Thu 2026-01-01 UTC
    private val now = clock.millis

    @Test
    fun rowsAreTheThreeMostRecentAndWeekCountsThisWeek() = runTest {
        val sources = FakeTemplateSources(
            templates = listOf("a", "b", "c", "d").map { template(it) },
            sessions = listOf(
                session("1", "a", now - 10 * DAY),
                session("2", "b", now - 1),
                session("3", "c", now - 2 * DAY),
                session("4", "d", now - 3 * DAY, durationSeconds = 600),
            ),
        )
        val state = HomeViewModel(sources, clock).uiState.first { it.loaded }
        assertEquals(listOf("b", "c", "d"), state.rows.map { it.id })
        assertEquals(LocalDate.of(2026, 1, 1), state.today)
        // Mon 29 Dec … : b (Wed 31), c (Tue 30), d (Mon 29). a was the week before.
        assertEquals(3, state.week!!.sessionCount)
    }

    @Test
    fun newUserSeesActivePlanAndEmptyRowsWithoutOne() = runTest {
        val sources = FakeTemplateSources(
            templates = listOf("a", "b").map { template(it, source = EntitySource.DEFAULT) },
            plans = listOf(plan("p", "b", "a")),
            activePlanId = "p",
        )
        val vm = HomeViewModel(sources, clock)
        assertEquals(listOf("b", "a"), vm.uiState.first { it.loaded }.rows.map { it.id })

        sources.activePlanId.value = null
        assertEquals(emptyList<String>(), vm.uiState.first { it.loaded && it.rows.isEmpty() }.rows)
    }

    @Test
    fun startSheetListsYoursByRecencyThenStartersInPlanOrder() = runTest {
        val sources = FakeTemplateSources(
            templates = listOf(
                template("mine", source = EntitySource.CUSTOM),
                template("s1", source = EntitySource.DEFAULT),
                template("s2", source = EntitySource.DEFAULT),
                template("s3", source = EntitySource.DEFAULT),
            ),
            sessions = listOf(session("x", "s2", now - DAY)),
            plans = listOf(plan("p", "s3", "s1", "s2")),
        )
        val state = HomeViewModel(sources, clock).uiState.first { it.loaded }
        // Done starter counts as yours; never-done custom after it.
        assertEquals(listOf("s2", "mine"), state.sheetYours.map { it.id })
        assertEquals(listOf("s3", "s1"), state.sheetStarters.map { it.id })
        assertEquals(1, state.sheetStarters.first().planDay)
    }
}
