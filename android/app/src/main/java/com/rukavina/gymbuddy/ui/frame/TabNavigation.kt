package com.rukavina.gymbuddy.ui.frame

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.rukavina.gymbuddy.navigation.NavRoutes
import com.rukavina.gymbuddy.ui.design.NavTab

/** Root route of each tab. */
val NavTab.route: String
    get() = when (this) {
        NavTab.Home -> NavRoutes.Home
        NavTab.Workouts -> NavRoutes.Workouts
        NavTab.Exercises -> NavRoutes.Exercises
        NavTab.Progress -> NavRoutes.Progress
        NavTab.You -> NavRoutes.You
    }

private val tabByRoute = NavTab.entries.associateBy { it.route }

/**
 * The tab that owns the current screen: the top-most tab root on the
 * back stack (pushed screens sit above their tab's root). Home when the
 * stack has none yet.
 */
fun selectedTab(backStackRoutes: List<String?>): NavTab =
    backStackRoutes.asReversed().firstNotNullOfOrNull { tabByRoute[it] } ?: NavTab.Home

/** The workout runs full screen: no nav bar, no live bar. */
fun isFullScreen(route: String?): Boolean = route == NavRoutes.ActiveWorkout

/**
 * Switch tab and keep each tab's own back stack: the old tab's screens
 * are saved, the new tab's restored. Reselecting a tab is a no-op.
 */
fun NavController.navigateToTab(tab: NavTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
