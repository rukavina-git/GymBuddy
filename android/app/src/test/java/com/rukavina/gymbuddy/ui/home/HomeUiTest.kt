package com.rukavina.gymbuddy.ui.home

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.usecase.template.TemplateStats
import com.rukavina.gymbuddy.testutil.TemplateFixtures.template
import com.rukavina.gymbuddy.ui.template.TemplateLine
import com.rukavina.gymbuddy.ui.template.page.TemplatePageContent
import com.rukavina.gymbuddy.ui.template.page.TemplatePageUiState
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

/** Home and template page interaction (Plan 7 05a). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class HomeUiTest {

    @get:Rule val rule = createComposeRule()

    private val today = LocalDate.of(2026, 1, 1)
    private val pushA = template("push", "Push A")
    private val legs = template("legs", "Legs")
    private val state = HomeUiState(
        loaded = true,
        week = TemplateStats.week(emptyList(), today, ZoneOffset.UTC),
        rows = listOf(TemplateLine(pushA, null, null), TemplateLine(legs, null, null)),
        today = today,
        zone = ZoneOffset.UTC,
    )

    private fun home(running: Boolean, onStart: (WorkoutTemplate) -> Unit = {}, onOpen: (String) -> Unit = {}) =
        rule.setContent {
            GymBuddyTheme { HomeContent(state, running, onStart, onOpen, {}, {}) }
        }

    @Test
    fun playStartsThatTemplateAndRowOpensIt() {
        val started = mutableListOf<String>()
        val opened = mutableListOf<String>()
        home(running = false, onStart = { started += it.id }, onOpen = { opened += it })
        // Robolectric's screen is small: scroll the row in before tapping.
        rule.onAllNodesWithContentDescription("Start")[1].performScrollTo().performClick()
        rule.onNodeWithText("Push A").performScrollTo().performClick()
        assertEquals(listOf("legs"), started)
        assertEquals(listOf("push"), opened)
    }

    @Test
    fun runningWorkoutHidesStartAndRows() {
        home(running = true)
        rule.onAllNodesWithText("Start workout").assertCountEquals(0)
        rule.onAllNodesWithText("Push A").assertCountEquals(0)
        rule.onAllNodesWithText("All templates").assertCountEquals(1)
    }

    @Test
    fun startWorkoutOpensSheetAndSheetRowStarts() {
        val started = mutableListOf<String>()
        rule.setContent {
            GymBuddyTheme {
                HomeContent(state.copy(sheetYours = listOf(TemplateLine(legs, null, null))), false, { started += it.id }, {}, {}, {})
            }
        }
        rule.onNodeWithText("Start workout").performClick()
        rule.onNodeWithText("Start a workout").assertExists()
        rule.onAllNodesWithText("Legs")[1].performClick()
        assertEquals(listOf("legs"), started)
    }

    @Test
    fun templatePageStartHidesWhileRunning() {
        val running = mutableStateOf(false)
        val started = mutableListOf<String>()
        rule.setContent {
            GymBuddyTheme {
                TemplatePageContent(
                    TemplatePageUiState(loaded = true, line = TemplateLine(pushA, null, null), today = today, zone = ZoneOffset.UTC),
                    workoutRunning = running.value, onBack = {}, onStart = { started += it.id },
                    onEdit = {}, onCopy = {}, onDelete = {}, onHide = {},
                )
            }
        }
        rule.onNodeWithText("Start workout").performClick()
        assertEquals(listOf("push"), started)
        running.value = true
        rule.onAllNodesWithText("Start workout").assertCountEquals(0)
    }
}
