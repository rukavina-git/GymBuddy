package com.rukavina.gymbuddy.ui.progress

import androidx.lifecycle.SavedStateHandle
import com.rukavina.gymbuddy.testutil.FakeTemplateSources
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import com.rukavina.gymbuddy.testutil.ProgressFixtures.day
import com.rukavina.gymbuddy.testutil.ProgressFixtures.lift
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AllSessionsViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val sources = FakeTemplateSources(
        sessions = listOf(
            day("a", 0, lift("bench", 80f to 5, name = "Bench"), title = "Push A"),
            day("b", 10, lift("squat", 100f to 5, name = "Squat"), title = "Legs"),
            day("c", 40, lift("bench", 85f to 5, name = "Bench"), title = "Push A"),
            day("d", 41, lift("bench", 90f to 1, name = "Bench"), title = "Gone", deletedAt = 1),
        ),
    )

    private fun vm(exerciseId: String? = null) =
        AllSessionsViewModel(SavedStateHandle(mapOf(AllSessionsViewModel.ARG_EXERCISE_ID to exerciseId)), sources)

    @Test
    fun monthsNewestFirstWithoutDeleted() = runTest {
        val s = vm().uiState.first { it.loaded }
        assertEquals(listOf(listOf("c"), listOf("b", "a")), s.months.map { m -> m.rows.map { it.session.id } })
        assertEquals(400f + 500f, s.months[1].volumeKg)
        assertNull(s.filter)
    }

    @Test
    fun searchMatchesWorkoutNamesWithTypos() = runTest {
        val vm = vm()
        vm.setQuery("pus")
        val s = vm.uiState.first { st -> st.loaded && st.months.flatMap { it.rows }.size == 2 }
        assertTrue(s.months.flatMap { it.rows }.all { it.session.title == "Push A" })
        vm.setQuery("leg day")
        val none = vm.uiState.first { st -> st.loaded && st.months.isEmpty() }
        assertTrue(none.hasAny)
    }

    @Test
    fun exerciseFilterFromTheRouteAndClear() = runTest {
        val vm = vm("squat")
        val s = vm.uiState.first { it.loaded }
        assertEquals("Squat", s.filter?.name)
        assertEquals(listOf("b"), s.months.flatMap { m -> m.rows.map { it.session.id } })
        vm.setExercise(null)
        assertEquals(3, vm.uiState.first { it.filter == null && it.loaded }.months.sumOf { it.rows.size })
    }

    @Test
    fun unknownExerciseIdShowsEverything() = runTest {
        val s = vm("nope").uiState.first { it.loaded }
        assertNull(s.filter)
        assertEquals(3, s.months.sumOf { it.rows.size })
    }
}
