package com.rukavina.gymbuddy.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    object Templates : BottomNavItem("templates", "Templates", Icons.Default.Assignment)
    object Exercises : BottomNavItem("exercises", "Exercises", Icons.Default.FitnessCenter)
    object Workouts : BottomNavItem("workouts", "Workouts", Icons.AutoMirrored.Filled.List)
    object Statistics : BottomNavItem("statistics", "Stats", Icons.Default.BarChart)
    object Profile : BottomNavItem("profile", "Profile", Icons.Default.Person)

    companion object {
        val items = listOf(Home, Templates, Exercises, Workouts, Statistics, Profile)
    }
}