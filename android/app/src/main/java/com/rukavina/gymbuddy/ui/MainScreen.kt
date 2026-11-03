package com.rukavina.gymbuddy.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import com.rukavina.gymbuddy.ui.theme.fadeThroughIn
import com.rukavina.gymbuddy.ui.theme.fadeThroughOut
import com.rukavina.gymbuddy.ui.theme.sharedAxisIn
import com.rukavina.gymbuddy.ui.theme.sharedAxisOut
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.rukavina.gymbuddy.ui.guest.SessionExpiredBanner
import com.rukavina.gymbuddy.ui.guest.AuthGateViewModel
import com.rukavina.gymbuddy.ui.guest.AccountDeletedDialog
import com.rukavina.gymbuddy.data.sync.AuthGateState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.rukavina.gymbuddy.BuildConfig
import com.rukavina.gymbuddy.ui.design.NavTab
import com.rukavina.gymbuddy.ui.frame.AppFrame
import com.rukavina.gymbuddy.ui.frame.isFullScreen
import com.rukavina.gymbuddy.ui.frame.navigateToTab
import com.rukavina.gymbuddy.ui.frame.route
import com.rukavina.gymbuddy.ui.frame.selectedTab
import com.rukavina.gymbuddy.ui.frame.toLiveBarState
import com.rukavina.gymbuddy.navigation.NavRoutes
import com.rukavina.gymbuddy.ui.exercise.ExerciseScreen
import com.rukavina.gymbuddy.ui.exercise.ExerciseDetailsScreen
import com.rukavina.gymbuddy.ui.exercise.ExerciseViewModel
import com.rukavina.gymbuddy.ui.profile.ProfileScreen
import com.rukavina.gymbuddy.ui.profile.ProfileViewModel
import com.rukavina.gymbuddy.ui.profile.edit.EditActivityLevelScreen
import com.rukavina.gymbuddy.ui.profile.edit.EditBirthdateScreen
import com.rukavina.gymbuddy.ui.profile.edit.EditBioScreen
import com.rukavina.gymbuddy.ui.profile.edit.EditFitnessGoalScreen
import com.rukavina.gymbuddy.ui.profile.edit.EditGenderScreen
import com.rukavina.gymbuddy.ui.profile.edit.EditHeightScreen
import com.rukavina.gymbuddy.ui.profile.edit.EditNameScreen
import com.rukavina.gymbuddy.ui.profile.edit.EditTargetWeightScreen
import com.rukavina.gymbuddy.ui.profile.edit.EditUnitsScreen
import com.rukavina.gymbuddy.ui.profile.edit.EditWeightScreen
import com.rukavina.gymbuddy.ui.guest.GuestMergeDialogs
import com.rukavina.gymbuddy.navigation.openWelcome
import com.rukavina.gymbuddy.ui.design.AppSnackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import com.rukavina.gymbuddy.ui.guest.GuestMergeViewModel
import com.rukavina.gymbuddy.ui.settings.AboutScreen
import com.rukavina.gymbuddy.ui.settings.AppPreferencesScreen
import com.rukavina.gymbuddy.ui.settings.HiddenExercisesScreen
import com.rukavina.gymbuddy.ui.settings.HiddenTemplatesScreen
import com.rukavina.gymbuddy.ui.settings.SettingsScreen
import com.rukavina.gymbuddy.ui.settings.debug.SyncDebugScreen
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutScreen
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutViewModel


@Composable
fun MainScreen(rootNavController: NavHostController) {
    val guestMergeViewModel: GuestMergeViewModel = hiltViewModel()
    val ownerReady by guestMergeViewModel.ownerReady.collectAsState()
    val mergeState by guestMergeViewModel.state.collectAsState()

    // Receipts and failures from the merge (Figma "14 · Receipt") show over the tabs.
    val snackbar = remember { SnackbarHostState() }
    GuestMergeDialogs(
        state = mergeState,
        snackbar = snackbar,
        onAdd = guestMergeViewModel::add,
        onCancel = guestMergeViewModel::cancel,
        onDismiss = guestMergeViewModel::dismiss
    )

    // Wait for the signed-in owner's file (ADR 0014): never show the
    // previous owner's data to the new one.
    val authGateViewModel: AuthGateViewModel = hiltViewModel()
    val gateState by authGateViewModel.state.collectAsState()
    val deletedCounts by authGateViewModel.counts.collectAsState()
    LaunchedEffect(Unit) {
        authGateViewModel.erased.collect { rootNavController.openWelcome() }
    }
    (gateState as? AuthGateState.AccountDeleted)?.let { deleted ->
        LaunchedEffect(deleted) { authGateViewModel.loadCounts() }
        AccountDeletedDialog(
            email = deleted.email,
            counts = deletedCounts,
            onKeep = authGateViewModel::keep,
            onErase = authGateViewModel::erase
        )
    }

    if (ownerReady) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (gateState is AuthGateState.SessionExpired) {
                SessionExpiredBanner(
                    onSignIn = { rootNavController.navigate(NavRoutes.signIn()) },
                    modifier = Modifier.statusBarsPadding()
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                MainContent(rootNavController, snackbar)
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun MainContent(rootNavController: NavHostController, snackbar: SnackbarHostState) {
    val bottomNavController = rememberNavController()
    val backStackEntry by bottomNavController.currentBackStackEntryAsState()
    val backStack by bottomNavController.currentBackStack.collectAsState()
    val currentRoute = backStackEntry?.destination?.route
    val tab = selectedTab(backStack.map { it.destination.route })

    // Create ViewModels at MainScreen level so they survive navigation
    val activeWorkoutViewModel: ActiveWorkoutViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val workoutState by activeWorkoutViewModel.uiState.collectAsState()

    val openWorkout = { bottomNavController.navigate(NavRoutes.ActiveWorkout) { launchSingleTop = true } }
    // Pop the workout first so no tab saves it into its back stack.
    val leaveWorkout = {
        bottomNavController.popBackStack(NavRoutes.ActiveWorkout, inclusive = true)
        bottomNavController.navigateToTab(NavTab.Workouts)
    }

    AppFrame(
        selectedTab = tab,
        onSelectTab = bottomNavController::navigateToTab,
        fullScreen = isFullScreen(currentRoute),
        liveBar = workoutState.toLiveBarState(),
        onLiveBarClick = openWorkout,
        snackbarHost = { SnackbarHost(snackbar) { AppSnackbar(it) } },
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = NavRoutes.Home,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { if (isTabSwitch()) fadeThroughIn else sharedAxisIn(forward = true) },
            exitTransition = { if (isTabSwitch()) fadeThroughOut else sharedAxisOut(forward = true) },
            popEnterTransition = { if (isTabSwitch()) fadeThroughIn else sharedAxisIn(forward = false) },
            popExitTransition = { if (isTabSwitch()) fadeThroughOut else sharedAxisOut(forward = false) },
        ) {
            composable(NavRoutes.Home) {
                HomeScreen(
                    activeWorkoutViewModel = activeWorkoutViewModel,
                    onNavigateToWorkout = openWorkout,
                    onNavigateToTemplates = { bottomNavController.navigateToTab(NavTab.Workouts) },
                    onNavigateToProfile = { bottomNavController.navigateToTab(NavTab.You) }
                )
            }
            composable(NavRoutes.Workouts) {
                CombinedWorkoutsScreen(
                    activeWorkoutViewModel = activeWorkoutViewModel,
                    onStartWorkout = openWorkout
                )
            }
            composable(NavRoutes.Exercises) {
                ExerciseScreen(navController = bottomNavController)
            }
            composable(
                route = NavRoutes.ExerciseDetails,
                arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""
                val exerciseViewModel: ExerciseViewModel = hiltViewModel()
                val uiState = exerciseViewModel.uiState.collectAsState().value
                val exercise = uiState.exercises.find { it.id == exerciseId }
                val note = exerciseViewModel.selectedExerciseNote.collectAsState().value

                LaunchedEffect(exerciseId) {
                    exerciseViewModel.loadExerciseNote(exerciseId)
                }

                if (exercise != null) {
                    ExerciseDetailsScreen(
                        exercise = exercise,
                        note = note,
                        onNavigateBack = { bottomNavController.navigateUp() },
                        onUpdateNote = { newNote ->
                            exerciseViewModel.updateExerciseNote(exerciseId, newNote)
                        }
                    )
                }
            }
            composable(NavRoutes.Progress) { StatisticsScreen() }
            composable(NavRoutes.You) {
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

            composable(NavRoutes.EditBirthdate) {
                val uiState = profileViewModel.uiState.collectAsState().value
                EditBirthdateScreen(
                    navController = bottomNavController,
                    currentBirthDate = uiState.birthDate,
                    onSave = { profileViewModel.onBirthDateSaved(it) }
                )
            }

            composable(NavRoutes.EditWeight) {
                val uiState = profileViewModel.uiState.collectAsState().value
                EditWeightScreen(
                    navController = bottomNavController,
                    currentWeight = uiState.weight ?: 70f,
                    preferredUnits = uiState.preferredUnits,
                    onSave = { profileViewModel.onWeightSaved(it) }
                )
            }

            composable(NavRoutes.EditHeight) {
                val uiState = profileViewModel.uiState.collectAsState().value
                EditHeightScreen(
                    navController = bottomNavController,
                    currentHeight = uiState.height ?: 170f,
                    preferredUnits = uiState.preferredUnits,
                    onSave = { profileViewModel.onHeightSaved(it) }
                )
            }

            composable(NavRoutes.EditTargetWeight) {
                val uiState = profileViewModel.uiState.collectAsState().value
                EditTargetWeightScreen(
                    navController = bottomNavController,
                    currentTargetWeight = uiState.targetWeight ?: 70f,
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

            composable(NavRoutes.HiddenTemplates) {
                HiddenTemplatesScreen(navController = bottomNavController)
            }

            composable(NavRoutes.AppPreferences) {
                AppPreferencesScreen(navController = bottomNavController)
            }
            if (BuildConfig.DEBUG) {
                composable(NavRoutes.SyncDebug) {
                    SyncDebugScreen(navController = bottomNavController)
                }
            }

            composable(NavRoutes.ActiveWorkout) {
                ActiveWorkoutScreen(
                    viewModel = activeWorkoutViewModel,
                    onWorkoutComplete = leaveWorkout,
                    onWorkoutDiscarded = leaveWorkout
                )
            }
        }
    }
}
/** Both ends are tab roots: fade-through, no slide (tabs have no order). */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch() =
    NavTab.entries.any { it.route == initialState.destination.route } &&
        NavTab.entries.any { it.route == targetState.destination.route }


