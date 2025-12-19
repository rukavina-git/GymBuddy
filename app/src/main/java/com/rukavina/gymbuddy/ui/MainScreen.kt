package com.rukavina.gymbuddy.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.rukavina.gymbuddy.ui.profile.ProfileViewModel
import com.rukavina.gymbuddy.ui.profile.edit.EditActivityLevelScreen
import com.rukavina.gymbuddy.ui.profile.edit.EditAgeScreen
import com.rukavina.gymbuddy.ui.profile.edit.EditBioScreen
import com.rukavina.gymbuddy.ui.profile.edit.EditFitnessGoalScreen
import com.rukavina.gymbuddy.ui.profile.edit.EditGenderScreen
import com.rukavina.gymbuddy.ui.profile.edit.EditHeightScreen
import com.rukavina.gymbuddy.ui.profile.edit.EditNameScreen
import com.rukavina.gymbuddy.ui.profile.edit.EditTargetWeightScreen
import com.rukavina.gymbuddy.ui.profile.edit.EditUnitsScreen
import com.rukavina.gymbuddy.ui.profile.edit.EditWeightScreen
import com.rukavina.gymbuddy.ui.settings.AboutScreen
import com.rukavina.gymbuddy.ui.settings.HiddenExercisesScreen
import com.rukavina.gymbuddy.ui.settings.SettingsScreen
import com.rukavina.gymbuddy.ui.template.WorkoutTemplateScreen
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutScreen
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutViewModel
import com.rukavina.gymbuddy.ui.workout.WorkoutScreen


@Composable
fun MainScreen(rootNavController: NavHostController) {
    val bottomNavController = rememberNavController()
    val backStackEntry = bottomNavController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry.value?.destination

    // Create ViewModels at MainScreen level so they survive navigation
    val activeWorkoutViewModel: ActiveWorkoutViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val bottomNavItems = listOf(
                    BottomNavItem.Home,
                    BottomNavItem.Templates,
                    BottomNavItem.Exercises,
                    BottomNavItem.Workouts,
                    BottomNavItem.Statistics,
                    BottomNavItem.Settings
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
                        label = { Text(item.label) },
                        alwaysShowLabel = false
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
                        bottomNavController.navigate(NavRoutes.Settings)
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
            composable(NavRoutes.Settings) {
                SettingsScreen(
                    bottomNavController = bottomNavController,
                    rootNavController = rootNavController
                )
            }

            composable(NavRoutes.About) {
                AboutScreen(navController = bottomNavController)
            }
            composable(NavRoutes.Profile) {
                ProfileScreen(
                    rootNavController = rootNavController,
                    bottomNavController = bottomNavController,
                    viewModel = profileViewModel
                )
            }

            composable(NavRoutes.EditName) {
                val uiState = profileViewModel.uiState.collectAsState().value
                EditNameScreen(
                    navController = bottomNavController,
                    currentName = uiState.name,
                    onSave = { profileViewModel.onNameSaved(it) }
                )
            }

            composable(NavRoutes.EditBio) {
                val uiState = profileViewModel.uiState.collectAsState().value
                EditBioScreen(
                    navController = bottomNavController,
                    currentBio = uiState.bio,
                    onSave = { profileViewModel.onBioSaved(it) }
                )
            }

            composable(NavRoutes.EditAge) {
                val uiState = profileViewModel.uiState.collectAsState().value
                EditAgeScreen(
                    navController = bottomNavController,
                    currentAge = uiState.age.toIntOrNull() ?: 25,
                    onSave = { profileViewModel.onAgeSaved(it) }
                )
            }

            composable(NavRoutes.EditWeight) {
                val uiState = profileViewModel.uiState.collectAsState().value
                EditWeightScreen(
                    navController = bottomNavController,
                    currentWeight = uiState.weight.toFloatOrNull() ?: 70f,
                    preferredUnits = uiState.preferredUnits,
                    onSave = { profileViewModel.onWeightSaved(it) }
                )
            }

            composable(NavRoutes.EditHeight) {
                val uiState = profileViewModel.uiState.collectAsState().value
                EditHeightScreen(
                    navController = bottomNavController,
                    currentHeight = uiState.height.toFloatOrNull() ?: 170f,
                    preferredUnits = uiState.preferredUnits,
                    onSave = { profileViewModel.onHeightSaved(it) }
                )
            }

            composable(NavRoutes.EditTargetWeight) {
                val uiState = profileViewModel.uiState.collectAsState().value
                EditTargetWeightScreen(
                    navController = bottomNavController,
                    currentTargetWeight = uiState.targetWeight.toFloatOrNull() ?: 70f,
                    preferredUnits = uiState.preferredUnits,
                    onSave = { profileViewModel.onTargetWeightSaved(it) }
                )
            }

            composable(NavRoutes.EditGender) {
                val uiState = profileViewModel.uiState.collectAsState().value
                EditGenderScreen(
                    navController = bottomNavController,
                    currentGender = uiState.gender,
                    onSave = { profileViewModel.onGenderSaved(it) }
                )
            }

            composable(NavRoutes.EditFitnessGoal) {
                val uiState = profileViewModel.uiState.collectAsState().value
                EditFitnessGoalScreen(
                    navController = bottomNavController,
                    currentGoal = uiState.fitnessGoal,
                    onSave = { profileViewModel.onFitnessGoalSaved(it) }
                )
            }

            composable(NavRoutes.EditActivityLevel) {
                val uiState = profileViewModel.uiState.collectAsState().value
                EditActivityLevelScreen(
                    navController = bottomNavController,
                    currentLevel = uiState.activityLevel,
                    onSave = { profileViewModel.onActivityLevelSaved(it) }
                )
            }

            composable(NavRoutes.EditUnits) {
                val uiState = profileViewModel.uiState.collectAsState().value
                EditUnitsScreen(
                    navController = bottomNavController,
                    currentUnits = uiState.preferredUnits,
                    onSave = { profileViewModel.onPreferredUnitsSaved(it) }
                )
            }

            composable(NavRoutes.HiddenExercises) {
                HiddenExercisesScreen(navController = bottomNavController)
            }

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