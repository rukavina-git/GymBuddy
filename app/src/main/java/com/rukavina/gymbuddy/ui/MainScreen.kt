package com.rukavina.gymbuddy.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rukavina.gymbuddy.navigation.BottomNavItem
import com.rukavina.gymbuddy.navigation.NavRoutes
import com.rukavina.gymbuddy.ui.exercise.ExerciseScreen
import com.rukavina.gymbuddy.ui.profile.ProfileScreen
import com.rukavina.gymbuddy.ui.template.WorkoutTemplateScreen
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutScreen
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutViewModel
import com.rukavina.gymbuddy.ui.workout.WorkoutScreen


@Composable
fun MainScreen(rootNavController: NavHostController) {
    val bottomNavController = rememberNavController()
    val backStackEntry = bottomNavController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry.value?.destination

    // Create ViewModel at MainScreen level so it survives navigation
    val activeWorkoutViewModel: ActiveWorkoutViewModel = hiltViewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val bottomNavItems = listOf(
                    BottomNavItem.Home,
                    BottomNavItem.Templates,
                    BottomNavItem.Exercises,
                    BottomNavItem.Workouts,
                    BottomNavItem.Statistics,
                    BottomNavItem.Profile
                )
                bottomNavItems.forEach { item ->
                    val selected =
                        currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                // Pop up to the main graph start destination to avoid building a big back stack
                                popUpTo(NavRoutes.Home) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = NavRoutes.Home,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(NavRoutes.Home) {
                HomeScreen(
                    activeWorkoutViewModel = activeWorkoutViewModel,
                    onNavigateToWorkout = {
                        bottomNavController.navigate(NavRoutes.ActiveWorkout)
                    },
                    onNavigateToTemplates = {
                        bottomNavController.navigate(NavRoutes.Templates)
                    },
                    onNavigateToProfile = {
                        bottomNavController.navigate(NavRoutes.Profile)
                    }
                )
            }
            composable(NavRoutes.Templates) {
                WorkoutTemplateScreen(
                    activeWorkoutViewModel = activeWorkoutViewModel,
                    onStartWorkout = { bottomNavController.navigate(NavRoutes.ActiveWorkout) }
                )
            }
            composable(NavRoutes.Exercises) { ExerciseScreen() }
            composable(NavRoutes.Workouts) { WorkoutScreen() }
            composable(NavRoutes.Statistics) { StatisticsScreen() }
            composable(NavRoutes.Profile) { ProfileScreen(rootNavController) }
            composable(NavRoutes.ActiveWorkout) {
                ActiveWorkoutScreen(
                    viewModel = activeWorkoutViewModel,
                    onWorkoutComplete = {
                        bottomNavController.navigate(NavRoutes.Workouts) {
                            popUpTo(NavRoutes.Templates) { inclusive = false }
                        }
                    },
                    onWorkoutDiscarded = {
                        bottomNavController.navigate(NavRoutes.Templates) {
                            popUpTo(NavRoutes.Templates) { inclusive = false }
                        }
                    }
                )
            }
        }
    }
}