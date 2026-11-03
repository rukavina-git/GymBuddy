package com.rukavina.gymbuddy.ui.workouts

import androidx.lifecycle.SavedStateHandle
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

class WorkoutsViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val clock = MutableClock()

    private val sources = FakeTemplateSources(
        templates = listOf(
            template("arms", "Arms and abs"),
            template("upper", "Upper body"),
            template("legs", "Legs", source = EntitySource.DEFAULT),
            template("push", "Push A", source = EntitySource.DEFAULT),
        ),
        sessions = listOf(session("s", "upper", clock.millis - DAY)),
        plans = listOf(plan("ppl", "push", "legs")),
    )

    @Test
    fun yoursByRecencyThenStartersByPlan() = runTest {
        val state = WorkoutsViewModel(sources, SavedStateHandle(), clock).uiState.first { it.loaded }
        assertEquals(listOf("upper", "arms"), state.yours.map { it.id })
        assertEquals(listOf("push", "legs"), state.starters.map { it.id })
    }

    @Test
    fun queryFiltersBothSectionsAndSurvivesInSavedState() = runTest {
        val saved = SavedStateHandle()
        val vm = WorkoutsViewModel(sources, saved, clock)
        vm.setQuery("  leg")
        val state = vm.uiState.first { it.loaded && it.query == "  leg" }
        assertEquals(emptyList<String>(), state.yours.map { it.id })
        assertEquals(listOf("legs"), state.starters.map { it.id })
        assertEquals("  leg", saved.get<String>("query"))
    }
}
