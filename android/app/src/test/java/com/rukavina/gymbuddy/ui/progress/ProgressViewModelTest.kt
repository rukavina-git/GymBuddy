package com.rukavina.gymbuddy.ui.progress

import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.testutil.FakeTemplateSources
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import com.rukavina.gymbuddy.testutil.ProgressFixtures.DAY
import com.rukavina.gymbuddy.testutil.ProgressFixtures.day
import com.rukavina.gymbuddy.testutil.ProgressFixtures.lift
import com.rukavina.gymbuddy.testutil.testPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ProgressViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val sources = FakeTemplateSources()
    private val preferences = AppPreferencesRepository(testPreferencesDataStore())
    private val clock = Clock.fixed(Instant.ofEpochMilli(30 * DAY), ZoneOffset.UTC)

    private fun vm() = ProgressViewModel(sources, preferences, clock)

    private val history = listOf(
        day("a", 0, lift("bench", 80f to 5, name = "Bench"), lift("squat", 100f to 5, name = "Squat")),
        day("b", 7, lift("bench", 82.5f to 5, name = "Bench"), lift("squat", 105f to 5, name = "Squat")),
        day("c", 14, lift("bench", 85f to 5, name = "Bench"), lift("squat", 110f to 5, name = "Squat")),
        day("d", 21, lift("bench", 87.5f to 5, name = "Bench")),
        day("e", 22, title = "Empty"),
        day("f", 23, lift("row", 60f to 8, name = "Row")),
    )

    @Test
    fun noSessionsIsEmpty() = runTest {
        val s = vm().uiState.first { it.loaded }
        assertTrue(s.empty)
        assertNull(s.chart)
    }

    @Test
    fun fullTabShowsMostLoggedLiftAndFiveRecent() = runTest {
        sources.sessions.value = history
        val s = vm().uiState.first { it.loaded }
        assertFalse(s.empty)
        assertEquals("bench", s.chart?.lift?.exerciseId) // 4 sessions beats squat's 3
        assertEquals(listOf("bench", "squat"), s.lifts.map { it.exerciseId })
        assertEquals(listOf("f", "e", "d", "c", "b"), s.recent.map { it.session.id })
        assertTrue(s.recent.first { it.session.id == "d" }.isPr)
        assertNull(s.recent.first { it.session.id == "e" }.top)
    }

    @Test
    fun notEnoughDataHasNoCard() = runTest {
        sources.sessions.value = history.take(2)
        val s = vm().uiState.first { it.loaded }
        assertFalse(s.empty)
        assertNull(s.chart)
    }

    @Test
    fun changeLiftIsKeptAndFallsBackWhenNotEligible() = runTest {
        sources.sessions.value = history
        val vm = vm()
        vm.uiState.first { it.loaded }
        vm.chooseLift("squat")
        assertEquals("squat", vm.uiState.first { it.chart?.lift?.exerciseId == "squat" }.chart?.lift?.exerciseId)
        // Squat drops under 3 sessions: the most-logged lift comes back.
        sources.sessions.value = history.filter { it.id != "c" }
        assertEquals("bench", vm.uiState.first { it.chart?.lift?.exerciseId != "squat" }.chart?.lift?.exerciseId)
    }

    @Test
    fun weekUsesTheClock() = runTest {
        // Clock day 30 = Saturday 31 Jan 1970; Monday is day 25.
        sources.sessions.value = history + day("g", 26, lift("bench", 50f to 10), minutes = 40)
        val s = vm().uiState.first { it.loaded }
        assertEquals(1, s.week.sessions)
        assertEquals(500f, s.week.volumeKg)
    }
}
