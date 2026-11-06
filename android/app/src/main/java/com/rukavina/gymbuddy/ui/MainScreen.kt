package com.rukavina.gymbuddy.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
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
import com.rukavina.gymbuddy.ui.progress.AllSessionsScreen
import com.rukavina.gymbuddy.ui.progress.AllSessionsViewModel
import com.rukavina.gymbuddy.ui.progress.ProgressScreen
import com.rukavina.gymbuddy.ui.progress.session.SessionDeleteViewModel
import com.rukavina.gymbuddy.ui.progress.session.SessionDetailScreen
import com.rukavina.gymbuddy.ui.progress.session.SessionDetailViewModel
import com.rukavina.gymbuddy.ui.progress.session.SessionEditScreen
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
import com.rukavina.gymbuddy.ui.guest.GuestMergeDialogs
import com.rukavina.gymbuddy.navigation.openWelcome
import com.rukavina.gymbuddy.ui.design.AppSnackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import com.rukavina.gymbuddy.ui.guest.GuestMergeViewModel
import com.rukavina.gymbuddy.ui.you.AboutScreen
import com.rukavina.gymbuddy.ui.you.HiddenExercisesScreen
import com.rukavina.gymbuddy.ui.you.HiddenTemplatesScreen
import com.rukavina.gymbuddy.ui.you.YouNav
import com.rukavina.gymbuddy.ui.you.YouScreen
import com.rukavina.gymbuddy.ui.you.body.BodyWeightScreen
import com.rukavina.gymbuddy.ui.you.profile.ProfileScreen
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
    val workoutState by activeWorkoutViewModel.uiState.collectAsState()

    val openWorkout = { bottomNavController.navigate(NavRoutes.ActiveWorkout) { launchSingleTop = true } }
    val workoutRunning by remember { derivedStateOf { workoutState.running } }
    val startTemplate: (WorkoutTemplate) -> Unit = { template ->
        // Start is hidden while a workout runs (Figma 85:10122), so there is nothing to replace.
        if (!activeWorkoutViewModel.hasActiveWorkout()) activeWorkoutViewModel.startWorkoutFromTemplate(template)
        openWorkout()
    }
    val openTemplate: (String) -> Unit = { id -> bottomNavController.navigate(NavRoutes.templatePage(id)) }
    val openSession: (String) -> Unit = { id -> bottomNavController.navigate(NavRoutes.sessionDetail(id)) }

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
    // Session Delete (Undo, R14-5): tombstoned at once, Undo restores it (step 09 decision 2).
    val sessionDeletes: SessionDeleteViewModel = hiltViewModel()
    val offerUndoSessionDelete: (String, String) -> Unit = { id, title ->
        sessionDeletes.delete(id)
        scope.launch {
            val result = snackbar.showSnackbar(
                message = context.getString(R.string.session_deleted, title),
                actionLabel = context.getString(R.string.undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) sessionDeletes.restore(id)
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
                arguments = listOf(
                    navArgument(ExercisePageViewModel.ARG_EXERCISE_ID) { type = NavType.StringType },
                    navArgument(ExercisePageViewModel.ARG_AT_PROGRESS) { type = NavType.BoolType; defaultValue = false },
                ),
            ) { entry ->
                val pageExerciseId = entry.arguments?.getString(ExercisePageViewModel.ARG_EXERCISE_ID)
                ExercisePageScreen(
                    onOpenSession = { id -> bottomNavController.navigate(NavRoutes.sessionDetail(id, pageExerciseId)) },
                    onAllSessions = { id -> bottomNavController.navigate(NavRoutes.allSessions(id)) },
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
            composable(NavRoutes.Progress) {
                ProgressScreen(
                    onOpenLift = { id -> bottomNavController.navigate(NavRoutes.exercisePage(id, atProgress = true)) },
                    onOpenSession = openSession,
                    onAllSessions = { bottomNavController.navigate(NavRoutes.allSessions()) },
                    onStartWorkout = { bottomNavController.navigateToTab(NavTab.Workouts) },
                )
            }
            composable(
                route = NavRoutes.AllSessions,
                arguments = listOf(
                    navArgument(AllSessionsViewModel.ARG_EXERCISE_ID) {
                        type = NavType.StringType; nullable = true; defaultValue = null
                    },
                ),
            ) { entry ->
                AllSessionsScreen(
                    onBack = { bottomNavController.popBackStack(entry.destination.id, inclusive = true) },
                    onOpenSession = openSession,
                )
            }
            composable(
                route = NavRoutes.SessionDetail,
                arguments = listOf(
                    navArgument(SessionDetailViewModel.ARG_SESSION_ID) { type = NavType.StringType },
                    navArgument(SessionDetailViewModel.ARG_EXERCISE_ID) {
                        type = NavType.StringType; nullable = true; defaultValue = null
                    },
                ),
            ) { entry ->
                // Pop only this entry, and only while it is on top (a gone session and Delete both leave).
                val leave = { if (bottomNavController.currentBackStackEntry == entry) bottomNavController.popBackStack() }
                SessionDetailScreen(
                    workoutRunning = workoutRunning,
                    onBack = { leave() },
                    onOpenExercise = { id -> bottomNavController.navigate(NavRoutes.exercisePage(id)) },
                    onStartAgain = startTemplate,
                    onEdit = { id -> bottomNavController.navigate(NavRoutes.sessionEdit(id)) },
                    onDelete = { id, title -> leave(); offerUndoSessionDelete(id, title) },
                )
            }
            composable(
                route = NavRoutes.SessionEdit,
                arguments = listOf(navArgument(SessionDetailViewModel.ARG_SESSION_ID) { type = NavType.StringType }),
            ) { entry ->
                SessionEditScreen(
                    onClose = { if (bottomNavController.currentBackStackEntry == entry) bottomNavController.popBackStack() },
                )
            }
            composable(NavRoutes.You) {
                YouScreen(
                    YouNav(
                        onProfile = { bottomNavController.navigate(NavRoutes.Profile) },
                        onCreateAccount = { rootNavController.navigate(NavRoutes.createAccount(CreateFrom.SETTINGS)) },
                        onSignIn = { rootNavController.navigate(NavRoutes.signIn()) },
                        onHiddenExercises = { bottomNavController.navigate(NavRoutes.HiddenExercises) },
                        onHiddenTemplates = { bottomNavController.navigate(NavRoutes.HiddenTemplates) },
                        onAbout = { bottomNavController.navigate(NavRoutes.About) },
                        onLeft = { rootNavController.openWelcome() },
                    ),
                )
            }

            composable(NavRoutes.About) {
                AboutScreen(
                    onBack = { bottomNavController.popBackStack() },
                    onSyncDebug = { bottomNavController.navigate(NavRoutes.SyncDebug) },
                )
            }
            composable(NavRoutes.Profile) {
                ProfileScreen(
                    onBack = { bottomNavController.popBackStack() },
                    onBodyWeight = { bottomNavController.navigate(NavRoutes.BodyWeight) },
                )
            }

            composable(NavRoutes.BodyWeight) {
                BodyWeightScreen(onBack = { bottomNavController.popBackStack() })
            }

            composable(NavRoutes.HiddenExercises) {
                HiddenExercisesScreen(onBack = { bottomNavController.popBackStack() })
            }

            composable(NavRoutes.HiddenTemplates) {
                HiddenTemplatesScreen(onBack = { bottomNavController.popBackStack() })
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


