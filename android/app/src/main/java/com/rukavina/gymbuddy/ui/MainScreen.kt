package com.rukavina.gymbuddy.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.ui.design.LargeTitleTopBar
import com.rukavina.gymbuddy.ui.home.HomeScreen
import com.rukavina.gymbuddy.ui.home.toRunningWorkout
import com.rukavina.gymbuddy.ui.template.TemplateDeleteViewModel
import com.rukavina.gymbuddy.ui.template.editor.TemplateEditorScreen
import com.rukavina.gymbuddy.ui.template.editor.TemplateEditorViewModel
import com.rukavina.gymbuddy.ui.template.picker.ExercisePickerScreen
import com.rukavina.gymbuddy.ui.template.picker.ExercisePickerViewModel
import com.rukavina.gymbuddy.ui.template.page.TemplatePageScreen
import com.rukavina.gymbuddy.ui.template.page.TemplatePageViewModel
import com.rukavina.gymbuddy.ui.workout.WorkoutScreen
import com.rukavina.gymbuddy.ui.workouts.WorkoutsScreen
import kotlinx.coroutines.launch
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
import com.rukavina.gymbuddy.ui.exercises.ExercisesScreen
import com.rukavina.gymbuddy.ui.exercises.create.ExerciseEditScreen
import com.rukavina.gymbuddy.ui.exercises.create.ExerciseEditViewModel
import com.rukavina.gymbuddy.ui.exercises.page.ExercisePageScreen
import com.rukavina.gymbuddy.ui.exercises.page.ExercisePageViewModel
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
import com.rukavina.gymbuddy.ui.workout.active.ActiveWorkoutScreen
import com.rukavina.gymbuddy.ui.workout.active.WorkoutSummaryScreen
import com.rukavina.gymbuddy.ui.auth.CreateFrom
import com.rukavina.gymbuddy.ui.theme.collapseIntoLiveBar
import com.rukavina.gymbuddy.ui.theme.growFromLiveBar
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
    val workoutRunning by remember { derivedStateOf { workoutState.running } }
    val startTemplate: (WorkoutTemplate) -> Unit = { template ->
        // Start is hidden while a workout runs (Figma 85:10122), so there is nothing to replace.
        if (!activeWorkoutViewModel.hasActiveWorkout()) activeWorkoutViewModel.startWorkoutFromTemplate(template)
        openWorkout()
    }
    val openTemplate: (String) -> Unit = { id -> bottomNavController.navigate(NavRoutes.templatePage(id)) }

    // Delete (Undo): the tombstone is written only when the snackbar goes without Undo.
    val scope = rememberCoroutineScope()
    val templateDeletes: TemplateDeleteViewModel = hiltViewModel()
    val context = LocalContext.current
    val offerUndoDelete: (String, String) -> Unit = { id, title ->
        scope.launch {
            var settled = false
            try {
                val result = snackbar.showSnackbar(
                    message = context.getString(R.string.template_deleted, title),
                    actionLabel = context.getString(R.string.undo),
                    duration = SnackbarDuration.Short,
                )
                settled = true
                if (result == SnackbarResult.ActionPerformed) templateDeletes.undo(id) else templateDeletes.commit(id)
            } finally {
                // Main left mid-snackbar (sign-out, owner switch): keep the template.
                if (!settled) templateDeletes.undo(id)
            }
        }
    }
    // Pop the workout (or its summary) first so no tab saves it into its back stack.
    val leaveWorkout = {
        bottomNavController.popBackStack(NavRoutes.ActiveWorkout, inclusive = true)
        bottomNavController.popBackStack(NavRoutes.WorkoutSummary, inclusive = true)
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
                    workoutRunning = workoutRunning,
                    onStartTemplate = startTemplate,
                    onOpenTemplate = openTemplate,
                    onAllTemplates = { bottomNavController.navigateToTab(NavTab.Workouts) },
                    onOpenWeek = { bottomNavController.navigateToTab(NavTab.Progress) },
                    workout = workoutState.toRunningWorkout(),
                    onOpenWorkout = openWorkout,
                    onEmptyWorkout = {
                        if (!activeWorkoutViewModel.hasActiveWorkout()) {
                            activeWorkoutViewModel.startEmptyWorkout(context.getString(R.string.workout_empty_title))
                        }
                        openWorkout()
                    },
                )
            }
            composable(NavRoutes.Workouts) {
                WorkoutsScreen(
                    onOpenTemplate = openTemplate,
                    onNewTemplate = { bottomNavController.navigate(NavRoutes.templateEditor()) },
                    onHiddenTemplates = { bottomNavController.navigate(NavRoutes.HiddenTemplates) },
                )
            }
            composable(
                route = NavRoutes.TemplatePage,
                arguments = listOf(
                    navArgument(TemplatePageViewModel.ARG_TEMPLATE_ID) { type = NavType.StringType },
                ),
            ) { entry ->
                TemplatePageScreen(
                    workoutRunning = workoutRunning,
                    onBack = { bottomNavController.popBackStack(entry.destination.id, inclusive = true) },
                    onStart = startTemplate,
                    onEdit = { id -> bottomNavController.navigate(NavRoutes.templateEditor(id)) },
                    // A copy replaces this page; a starter's pencil copy then opens its editor on top.
                    onOpenCopy = { id, edit ->
                        bottomNavController.navigate(NavRoutes.templatePage(id)) {
                            popUpTo(entry.destination.id) { inclusive = true }
                        }
                        if (edit) bottomNavController.navigate(NavRoutes.templateEditor(id))
                    },
                    onDeleted = { id, title ->
                        bottomNavController.popBackStack(entry.destination.id, inclusive = true)
                        offerUndoDelete(id, title)
                    },
                    onMessage = { message -> scope.launch { snackbar.showSnackbar(message) } },
                )
            }
            composable(
                route = NavRoutes.TemplateEditor,
                arguments = listOf(
                    navArgument(TemplateEditorViewModel.ARG_TEMPLATE_ID) {
                        type = NavType.StringType; nullable = true; defaultValue = null
                    },
                ),
            ) { entry ->
                val picked by entry.savedStateHandle
                    .getStateFlow<ArrayList<String>?>(ExercisePickerViewModel.RESULT_PICKED, null)
                    .collectAsState()
                TemplateEditorScreen(
                    onClose = { bottomNavController.popBackStack(entry.destination.id, inclusive = true) },
                    onAddExercises = { ids -> bottomNavController.navigate(NavRoutes.exercisePicker(ids)) },
                    picked = picked,
                    onPickedHandled = { entry.savedStateHandle[ExercisePickerViewModel.RESULT_PICKED] = null },
                )
            }
            composable(
                route = NavRoutes.ExercisePicker,
                arguments = listOf(
                    navArgument(ExercisePickerViewModel.ARG_IN_TEMPLATE) { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                ExercisePickerScreen(
                    onBack = { bottomNavController.popBackStack(entry.destination.id, inclusive = true) },
                    onAdd = { ids ->
                        bottomNavController.previousBackStackEntry?.savedStateHandle
                            ?.set(ExercisePickerViewModel.RESULT_PICKED, ArrayList(ids))
                        bottomNavController.popBackStack(entry.destination.id, inclusive = true)
                    },
                )
            }
            composable(NavRoutes.Exercises) {
                ExercisesScreen(
                    onOpen = { id -> bottomNavController.navigate(NavRoutes.exercisePage(id)) },
                    onNew = { bottomNavController.navigate(NavRoutes.exerciseEdit()) },
                    onHidden = { bottomNavController.navigate(NavRoutes.HiddenExercises) },
                    onMessage = { message -> scope.launch { snackbar.showSnackbar(message) } },
                )
            }
            composable(
                route = NavRoutes.ExercisePage,
                arguments = listOf(navArgument(ExercisePageViewModel.ARG_EXERCISE_ID) { type = NavType.StringType }),
            ) { entry ->
                ExercisePageScreen(
                    onBack = { bottomNavController.popBackStack(entry.destination.id, inclusive = true) },
                    onEdit = { id -> bottomNavController.navigate(NavRoutes.exerciseEdit(id)) },
                    onMessage = { message -> scope.launch { snackbar.showSnackbar(message) } },
                )
            }
            composable(
                route = NavRoutes.ExerciseEdit,
                arguments = listOf(
                    navArgument(ExerciseEditViewModel.ARG_EXERCISE_ID) {
                        type = NavType.StringType; nullable = true; defaultValue = null
                    },
                ),
            ) { entry ->
                ExerciseEditScreen(
                    onClose = { bottomNavController.popBackStack(entry.destination.id, inclusive = true) },
                    // A new exercise opens its page in the editor's place; an edit goes back to the page.
                    onSaved = { id, isNew ->
                        if (isNew) {
                            bottomNavController.navigate(NavRoutes.exercisePage(id)) {
                                popUpTo(entry.destination.id) { inclusive = true }
                            }
                        } else {
                            bottomNavController.popBackStack(entry.destination.id, inclusive = true)
                        }
                    },
                    onMessage = { message -> scope.launch { snackbar.showSnackbar(message) } },
                )
            }
            // Old session list until the Progress tab lands (7.8).
            composable(NavRoutes.Progress) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column {
                        LargeTitleTopBar(title = stringResource(R.string.tab_progress))
                        WorkoutScreen()
                    }
                }
            }
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

            // ⌄ / Back shrink the workout into the live bar; the bar grows it back (motion.md #10).
            composable(
                NavRoutes.ActiveWorkout,
                enterTransition = { growFromLiveBar },
                popExitTransition = { collapseIntoLiveBar },
            ) { entry ->
                val picked by entry.savedStateHandle
                    .getStateFlow<ArrayList<String>?>(ExercisePickerViewModel.RESULT_PICKED, null)
                    .collectAsState()
                ActiveWorkoutScreen(
                    viewModel = activeWorkoutViewModel,
                    onAddExercises = { ids -> bottomNavController.navigate(NavRoutes.exercisePicker(ids)) },
                    picked = picked,
                    onPickedHandled = { entry.savedStateHandle[ExercisePickerViewModel.RESULT_PICKED] = null },
                    onCollapse = { bottomNavController.popBackStack(NavRoutes.ActiveWorkout, inclusive = true) },
                    onSaved = {
                        bottomNavController.navigate(NavRoutes.WorkoutSummary) {
                            popUpTo(NavRoutes.ActiveWorkout) { inclusive = true }
                        }
                    },
                    onDiscarded = leaveWorkout,
                )
            }
            composable(NavRoutes.WorkoutSummary) {
                WorkoutSummaryScreen(
                    viewModel = activeWorkoutViewModel,
                    onDone = leaveWorkout,
                    onCreateAccount = { rootNavController.navigate(NavRoutes.createAccount(CreateFrom.SETTINGS)) },
                )
            }
        }
    }
}
/** Both ends are tab roots: fade-through, no slide (tabs have no order). */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch() =
    NavTab.entries.any { it.route == initialState.destination.route } &&
        NavTab.entries.any { it.route == targetState.destination.route }


