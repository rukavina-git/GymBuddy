package com.rukavina.gymbuddy.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    object Workouts : BottomNavItem("workouts", "Workouts", Icons.Default.Assignment)
    object Exercises : BottomNavItem("exercises", "Exercises", Icons.Default.FitnessCenter)
    object Statistics : BottomNavItem("statistics", "Stats", Icons.Default.BarChart)
    object Settings : BottomNavItem("settings", "Settings", Icons.Default.Settings)

    companion object {
        val items = listOf(Home, Workouts, Exercises, Statistics, Settings)
    }
}