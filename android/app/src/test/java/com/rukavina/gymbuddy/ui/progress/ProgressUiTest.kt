package com.rukavina.gymbuddy.ui.progress

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performScrollToKey
import com.rukavina.gymbuddy.domain.progress.Lift
import com.rukavina.gymbuddy.domain.progress.ProgressStats
import com.rukavina.gymbuddy.testutil.ProgressFixtures.day
import com.rukavina.gymbuddy.testutil.ProgressFixtures.lift
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneOffset

/** Progress tab states (Plan 7 step 08, R7-1/2/3). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ProgressUiTest {

    @get:Rule val rule = createComposeRule()

    private val sessions = listOf(
        day("a", 0, lift("bench", 80f to 5, name = "Bench press"), title = "Push A"),
        day("b", 7, lift("bench", 82.5f to 5, name = "Bench press"), title = "Push A"),
        day("c", 14, lift("bench", 85f to 5, name = "Bench press"), title = "Push A"),
    )

    private fun show(state: ProgressUiState, onStart: () -> Unit = {}, onLift: (String) -> Unit = {}, onSession: (String) -> Unit = {}) =
        rule.setContent {
            GymBuddyTheme { ProgressContent(state, onLift, {}, onSession, {}, onStart) }
        }

    private fun state(withChart: Boolean) = ProgressUiState(
        loaded = true,
        empty = false,
        chart = if (withChart) ProgressStats.progress(Lift("bench", "Bench press", 3), sessions) else null,
        recent = sessionRows(sessions.reversed(), sessions),
        zone = ZoneOffset.UTC,
    )

    @Test
    fun emptyStateStartsAWorkout() {
        var started = 0
        show(ProgressUiState(loaded = true, empty = true), onStart = { started++ })
        rule.onNodeWithText("No workouts yet").assertExists()
        rule.onNodeWithText("Start a workout").performClick()
        assertEquals(1, started)
    }

    @Test
    fun noCardWithoutAnEligibleLift() {
        show(state(withChart = false))
        rule.onAllNodesWithText("Change lift").assertCountEquals(0)
        rule.onNodeWithText("Recent").assertExists()
    }

    @Test
    fun cardOpensLiftAndRowOpensSession() {
        val lifts = mutableListOf<String>()
        val opened = mutableListOf<String>()
        show(state(withChart = true), onLift = { lifts += it }, onSession = { opened += it })
        rule.onNodeWithText("You lift 5 kg more than 2 weeks ago.").performClick()
        assertEquals(listOf("bench"), lifts)
        rule.onNode(hasScrollAction()).performScrollToKey("c")
        rule.onAllNodesWithText("Push A")[0].performClick()
        assertEquals(listOf("c"), opened)
    }
}
