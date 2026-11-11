package com.rukavina.gymbuddy.ui.frame

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rukavina.gymbuddy.navigation.NavRoutes
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** F5 (11o): left-edge swipe back for 3-button navigation. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EdgeSwipeBackTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun startsInEdge_onlyInsideZone() {
        assertTrue(startsInEdge(0f, 60f))
        assertTrue(startsInEdge(60f, 60f))
        assertFalse(startsInEdge(61f, 60f))
    }

    @Test
    fun commitsBack_byDistanceOrFling() {
        assertTrue(commitsBack(dragX = 400f, widthPx = 1000f, velocityX = 0f, flingPx = 2000f))
        assertFalse(commitsBack(dragX = 300f, widthPx = 1000f, velocityX = 0f, flingPx = 2000f))
        assertTrue(commitsBack(dragX = 50f, widthPx = 1000f, velocityX = 2500f, flingPx = 2000f))
        assertFalse(commitsBack(dragX = 0f, widthPx = 1000f, velocityX = 2500f, flingPx = 2000f))
    }

    @Test
    fun enabled_offOnTabRootsAndWorkout() {
        assertFalse(edgeSwipeBackEnabled(NavRoutes.Home))
        assertFalse(edgeSwipeBackEnabled(NavRoutes.Progress))
        assertFalse(edgeSwipeBackEnabled(NavRoutes.ActiveWorkout))
        assertFalse(edgeSwipeBackEnabled(null))
        assertTrue(edgeSwipeBackEnabled(NavRoutes.TemplateEditor))
        assertTrue(edgeSwipeBackEnabled(NavRoutes.SessionEdit))
    }

    private lateinit var nav: NavHostController
    private var guardHits by mutableIntStateOf(0)

    private fun setUp(systemBackGesture: Boolean = false, guard: Boolean = false) {
        rule.setContent {
            GymBuddyTheme {
                nav = rememberNavController()
                EdgeSwipeBack(enabled = true, modifier = Modifier.testTag("root"), systemBackGesture = systemBackGesture) {
                    NavHost(nav, startDestination = "a") {
                        composable("a") { Box(Modifier.fillMaxSize()) { Text("A") } }
                        composable("b") {
                            if (guard) BackHandler { guardHits++ }
                            Box(Modifier.fillMaxSize()) { Text("B") }
                        }
                    }
                }
            }
        }
        rule.runOnIdle { nav.navigate("b") }
        rule.waitForIdle()
    }

    private fun swipeFrom(x: Float) {
        rule.onNodeWithTag("root").performTouchInput {
            swipe(Offset(x, centerY), Offset(x + width * 0.6f, centerY), durationMillis = 300)
        }
        rule.waitForIdle()
    }

    private fun route() = nav.currentBackStackEntry?.destination?.route

    @Test
    fun edgeSwipe_pops() {
        setUp()
        swipeFrom(2f)
        assertEquals("a", route())
    }

    @Test
    fun swipeOutsideEdge_staysPut() {
        setUp()
        swipeFrom(300f)
        assertEquals("b", route())
    }

    @Test
    fun edgeSwipe_runsBackHandlerGuard() {
        setUp(guard = true)
        swipeFrom(2f)
        assertEquals(1, guardHits)
        assertEquals("b", route())
    }

    @Test
    fun gestureNavigation_leavesItToTheSystem() {
        setUp(systemBackGesture = true)
        swipeFrom(2f)
        assertEquals("b", route())
    }
}
