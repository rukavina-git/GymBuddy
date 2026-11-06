package com.rukavina.gymbuddy.ui.frame

import com.rukavina.gymbuddy.navigation.NavRoutes
import com.rukavina.gymbuddy.ui.design.NavTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabNavigationTest {

    @Test
    fun everyTabHasItsOwnRoot() {
        assertEquals(NavTab.entries.size, NavTab.entries.map { it.route }.toSet().size)
    }

    @Test
    fun selectedTabIsTopMostTabRoot() {
        assertEquals(NavTab.Home, selectedTab(emptyList()))
        assertEquals(NavTab.Home, selectedTab(listOf(null, NavRoutes.Home)))
        assertEquals(
            NavTab.Exercises,
            selectedTab(listOf(null, NavRoutes.Home, NavRoutes.Exercises, NavRoutes.ExercisePage)),
        )
        assertEquals(NavTab.You, selectedTab(listOf(NavRoutes.Home, NavRoutes.You, NavRoutes.Profile, NavRoutes.EditName)))
    }

    @Test
    fun onlyTheWorkoutIsFullScreen() {
        assertTrue(isFullScreen(NavRoutes.ActiveWorkout))
        assertFalse(isFullScreen(NavRoutes.Home))
        assertFalse(isFullScreen(null))
    }
}
