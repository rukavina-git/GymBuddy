package com.rukavina.gymbuddy.ui.frame

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rukavina.gymbuddy.navigation.NavRoutes
import com.rukavina.gymbuddy.ui.design.AppNavBar
import com.rukavina.gymbuddy.ui.design.LargeTitleState
import com.rukavina.gymbuddy.ui.design.LargeTitleTopBar
import com.rukavina.gymbuddy.ui.design.NavTab
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** App frame (step 03): tabs, full-screen workout, live bar, big title. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppFrameTest {

    @get:Rule val rule = createComposeRule()

    private val bar = LiveBarState(
        title = "Push A · Bench press", setNumber = 3, setCount = 4,
        allDone = false, elapsedSeconds = 2292, progress = 0.5f,
    )

    private fun frame(fullScreen: Boolean, liveBar: LiveBarState?, onLiveBar: () -> Unit = {}) =
        rule.setContent {
            GymBuddyTheme {
                AppFrame(NavTab.Home, {}, fullScreen, liveBar, onLiveBar) { Text("content") }
            }
        }

    @Test
    fun liveBarShowsSetAndElapsedAndOpensWorkout() {
        var opened = 0
        frame(fullScreen = false, liveBar = bar) { opened++ }

        rule.onNodeWithText("Set 3 of 4 · 38:12").assertExists()
        rule.onNodeWithText("Push A · Bench press").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun noWorkoutNoLiveBar() {
        frame(fullScreen = false, liveBar = null)

        rule.onNodeWithText("Home").assertExists()
        rule.onNodeWithText("Push A · Bench press").assertDoesNotExist()
    }

    @Test
    fun fullScreenHidesNavBarAndLiveBar() {
        frame(fullScreen = true, liveBar = bar)

        rule.onNodeWithText("content").assertExists()
        rule.onNodeWithText("Home").assertDoesNotExist()
        rule.onNodeWithText("Push A · Bench press").assertDoesNotExist()
    }

    /** Real NavHost with stub screens, driven like MainScreen. */
    private fun tabs(): () -> NavHostController {
        lateinit var nav: NavHostController
        rule.setContent {
            GymBuddyTheme {
                nav = rememberNavController()
                val stack by nav.currentBackStack.collectAsState()
                AppFrame(
                    selectedTab(stack.map { it.destination.route }), nav::navigateToTab,
                    fullScreen = false, liveBar = null, onLiveBarClick = {},
                ) {
                    NavHost(nav, startDestination = NavRoutes.Home) {
                        NavTab.entries.forEach { tab -> composable(tab.route) { Text("root ${tab.route}") } }
                        composable(NavRoutes.ExercisePage) { Text("details") }
                    }
                }
            }
        }
        return { nav }
    }

    @Test
    fun tabTapShowsRootAndReselectKeepsOneEntry() {
        val nav = tabs()

        rule.onNodeWithText("Progress").performClick()
        rule.onNodeWithText("root progress").assertExists()
        rule.onNodeWithText("Progress").assertIsSelected()
        rule.onNodeWithText("Progress").performClick()
        rule.waitForIdle()

        val routes = nav().currentBackStack.value.map { it.destination.route }
        assertEquals(1, routes.count { it == NavRoutes.Progress })
    }

    @Test
    fun eachTabKeepsItsOwnStack() {
        val nav = tabs()

        rule.onNodeWithText("Exercises").performClick()
        rule.runOnIdle { nav().navigate(NavRoutes.exercisePage("x")) }
        rule.onNodeWithText("details").assertExists()
        rule.onNodeWithText("Exercises").assertIsSelected()

        rule.onNodeWithText("Home").performClick()
        rule.onNodeWithText("root home").assertExists()
        rule.onNodeWithText("Exercises").performClick()
        rule.onNodeWithText("details").assertExists()
    }

    @Test
    fun bigFontShowsIconsOnlyButKeepsNames() {
        rule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale = 2f)) {
                GymBuddyTheme { AppNavBar(NavTab.Home, {}) }
            }
        }

        rule.onAllNodesWithText("Exercises").assertCountEquals(0)
        NavTab.entries.forEach { rule.onNodeWithContentDescription(it.name).assertExists() }
    }

    @Test
    fun largeTitleCollapsesOnScrollAndStaysOneHeading() {
        val state = LargeTitleState()
        rule.setContent {
            GymBuddyTheme {
                androidx.compose.foundation.layout.Column {
                    LargeTitleTopBar("Workouts", state = state)
                    LazyColumn(Modifier.fillMaxSize().nestedScroll(state.nestedScrollConnection).testTag("list")) {
                        items(100) { Text("row $it") }
                    }
                }
            }
        }

        assertEquals(0f, state.collapsedFraction)
        rule.onNodeWithTag("list").performTouchInput { swipeUp() }
        rule.runOnIdle { assertEquals(1f, state.collapsedFraction) }
        rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertCountEquals(1)
        rule.onAllNodesWithText("Workouts").assertCountEquals(1)
    }
}
