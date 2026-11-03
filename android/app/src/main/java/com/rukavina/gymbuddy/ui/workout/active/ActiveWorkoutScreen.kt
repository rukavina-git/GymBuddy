package com.rukavina.gymbuddy.ui.workout.active

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.Equipment
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.ui.design.AppBottomSheet
import com.rukavina.gymbuddy.ui.design.AppDialog
import com.rukavina.gymbuddy.ui.design.AppMenu
import com.rukavina.gymbuddy.ui.design.AppMenuItem
import com.rukavina.gymbuddy.ui.design.AppSnackbar
import com.rukavina.gymbuddy.ui.design.InfoRow
import com.rukavina.gymbuddy.ui.design.Keypad
import com.rukavina.gymbuddy.ui.design.KeypadInput
import com.rukavina.gymbuddy.ui.design.KeypadValue
import com.rukavina.gymbuddy.ui.design.PrimaryButton
import com.rukavina.gymbuddy.ui.design.RestLine
import com.rukavina.gymbuddy.ui.design.SetCard
import com.rukavina.gymbuddy.ui.design.SetRow
import com.rukavina.gymbuddy.ui.design.SetState
import com.rukavina.gymbuddy.ui.design.ValuePanel
import com.rukavina.gymbuddy.ui.design.WorkoutTopBar
import com.rukavina.gymbuddy.ui.design.keypadSeparator
import com.rukavina.gymbuddy.ui.exercise.equipmentLabel
import com.rukavina.gymbuddy.ui.frame.formatElapsed
import com.rukavina.gymbuddy.ui.template.clock
import com.rukavina.gymbuddy.ui.template.joinDot
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.Durations
import com.rukavina.gymbuddy.ui.theme.Easings
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.theme.Haptic
import com.rukavina.gymbuddy.ui.theme.motionTween
import com.rukavina.gymbuddy.ui.theme.rememberHaptics
import com.rukavina.gymbuddy.ui.theme.sharedAxisIn
import com.rukavina.gymbuddy.ui.theme.sharedAxisOut
import com.rukavina.gymbuddy.ui.workout.ActiveExercise
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutUiState
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutViewModel
import com.rukavina.gymbuddy.ui.workout.FinishPreview
import com.rukavina.gymbuddy.ui.workout.SetField
import com.rukavina.gymbuddy.ui.workout.SetFields
import com.rukavina.gymbuddy.ui.workout.WorkoutDialog
import com.rukavina.gymbuddy.ui.workout.WorkoutEvent
import com.rukavina.gymbuddy.ui.workout.WorkoutKeypad
import com.rukavina.gymbuddy.ui.workout.WorkoutSet
import kotlinx.coroutines.launch

/**
 * The running workout (Plan 7 06a, Figma 67:6561): ⌄ / title / clock /
 * "3 / 15" / ⋯, exercise tabs, the exercise's sets with the current one
 * as a [SetCard], rest under the last done set, Up next, lime Done.
 * [onCollapse] minimises into the live bar (⌄ and system Back).
 */
@Composable
fun ActiveWorkoutScreen(
    viewModel: ActiveWorkoutViewModel,
    onCollapse: () -> Unit,
    onSaved: () -> Unit,
    onDiscarded: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = rememberHaptics()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                WorkoutEvent.SetDone -> haptics(Haptic.Confirm)
                WorkoutEvent.Rejected -> haptics(Haptic.Reject)
                is WorkoutEvent.SaveFailed -> {
                    haptics(Haptic.SaveFailed)
                    viewModel.clearError()
                    scope.launch {
                        snackbar.showSnackbar(
                            context.getString(
                                if (event.message == ActiveWorkoutViewModel.NOTHING_LOGGED) R.string.workout_nothing_logged
                                else R.string.workout_save_failed,
                            ),
                        )
                    }
                }
                is WorkoutEvent.SetDeleted -> scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    val result = snackbar.showSnackbar(
                        message = context.getString(R.string.workout_set_deleted),
                        actionLabel = context.getString(R.string.undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.setId)
                }
            }
        }
    }
    LaunchedEffect(state.summary != null) { if (state.summary != null) onSaved() }
    LaunchedEffect(state.workoutDiscarded) { if (state.workoutDiscarded) onDiscarded() }
    BackHandler(enabled = state.running) { onCollapse() }

    ActiveWorkoutContent(
        state = state,
        actions = WorkoutActions(
            onCollapse = onCollapse,
            onSelectExercise = viewModel::selectExercise,
            onStep = viewModel::step,
            onOpenKeypad = viewModel::openKeypad,
            onDone = viewModel::done,
            onDelete = viewModel::deleteSet,
            onUpNext = viewModel::nextExercise,
            onFinish = viewModel::requestFinish,
            onDiscard = viewModel::requestDiscard,
        ),
        snackbar = snackbar,
    )

    state.keypad?.let { keypad ->
        val separator = keypadSeparator()
        WorkoutKeypadSheet(keypad, state, viewModel.nextField(keypad)) { viewModel.keypad(it, separator) }
    }
    when (state.dialog) {
        WorkoutDialog.Finish -> FinishDialog(
            title = state.workoutTitle,
            preview = viewModel.finishPreview(),
            units = state.preferredUnits,
            onFinish = viewModel::saveWorkout,
            onKeepGoing = viewModel::dismissDialog,
        )
        WorkoutDialog.Discard -> AppDialog(
            title = stringResource(R.string.workout_discard_title),
            body = stringResource(R.string.workout_discard_body),
            confirmLabel = stringResource(R.string.workout_discard),
            onConfirm = viewModel::discardWorkout,
            dismissLabel = stringResource(R.string.workout_keep_going),
            onDismiss = viewModel::dismissDialog,
            destructive = true,
        )
        null -> Unit
    }
}

internal data class WorkoutActions(
    val onCollapse: () -> Unit = {},
    val onSelectExercise: (Int) -> Unit = {},
    val onStep: (SetField, Boolean) -> Unit = { _, _ -> },
    val onOpenKeypad: (SetField) -> Unit = {},
    val onDone: () -> Unit = {},
    val onDelete: (exerciseId: String, setId: String) -> Unit = { _, _ -> },
    val onUpNext: () -> Unit = {},
    val onFinish: () -> Unit = {},
    val onDiscard: () -> Unit = {},
)

@Composable
internal fun ActiveWorkoutContent(
    state: ActiveWorkoutUiState,
    actions: WorkoutActions,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    val d = GymBuddyTheme.dimens
    val exercise = state.currentExercise
    var menuOpen by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                Box {
                    WorkoutTopBar(
                        title = state.workoutTitle,
                        elapsed = formatElapsed(state.elapsedSeconds),
                        progress = "${state.currentExerciseIndex + 1} / ${state.exercises.size}",
                        onCollapse = actions.onCollapse,
                        // Hidden until 06b adds the exercise list sheet.
                        onProgress = null,
                        onMore = { menuOpen = true },
                    )
                    Box(Modifier.align(Alignment.TopEnd)) {
                        AppMenu(expanded = menuOpen, onDismiss = { menuOpen = false }) {
                            AppMenuItem(stringResource(R.string.workout_menu_finish), { menuOpen = false; actions.onFinish() })
                            AppMenuItem(
                                stringResource(R.string.workout_menu_discard),
                                { menuOpen = false; actions.onDiscard() },
                                danger = true,
                            )
                        }
                    }
                }
                ExerciseTabs(state.exercises, state.currentExerciseIndex, actions.onSelectExercise)
            }
        },
        bottomBar = {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = d.insetScreen, vertical = d.group),
                verticalArrangement = Arrangement.spacedBy(d.stack),
            ) {
                UpNextRow(state, actions.onUpNext)
                PrimaryButton(
                    text = doneLabel(state),
                    onClick = actions.onDone,
                    icon = AppIcons.Ui.Check,
                    loading = state.isLoading,
                    enabled = exercise != null && !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) { AppSnackbar(it) } },
    ) { padding ->
        // motion.md #15: the next exercise slides in sideways with its tab.
        AnimatedContent(
            targetState = state.currentExerciseIndex,
            transitionSpec = { sharedAxisIn(targetState > initialState) togetherWith sharedAxisOut(targetState > initialState) },
            label = "exercise",
            modifier = Modifier.padding(padding),
        ) { index ->
            val shown = state.exercises.getOrNull(index)
            if (shown != null) ExerciseSets(shown, state, actions)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseTabs(exercises: List<ActiveExercise>, selected: Int, onSelect: (Int) -> Unit) {
    if (exercises.isEmpty()) return
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    SecondaryScrollableTabRow(
        selectedTabIndex = selected.coerceIn(exercises.indices),
        containerColor = cs.background,
        contentColor = cs.onSurface,
        edgePadding = d.insetScreen,
        indicator = {
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(selected.coerceIn(exercises.indices), matchContentSize = true),
                color = GymBuddyTheme.colors.cobalt,
            )
        },
        divider = {},
    ) {
        exercises.forEachIndexed { i, ex ->
            Tab(
                selected = i == selected,
                onClick = { onSelect(i) },
                selectedContentColor = cs.onSurface,
                unselectedContentColor = cs.onSurfaceVariant,
                text = {
                    Row(horizontalArrangement = Arrangement.spacedBy(d.hairline), verticalAlignment = Alignment.CenterVertically) {
                        if (ex.allDone && ex.sets.isNotEmpty()) {
                            Icon(painterResource(AppIcons.Ui.Check), null, Modifier.size(d.iconSmall), tint = cs.primary)
                        }
                        Text(ex.exerciseName, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
            )
        }
    }
}

@Composable
private fun ExerciseSets(exercise: ActiveExercise, state: ActiveWorkoutUiState, actions: WorkoutActions) {
    val d = GymBuddyTheme.dimens
    val current = exercise.currentSet
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = d.insetScreen, vertical = d.group),
        verticalArrangement = Arrangement.spacedBy(d.stackTight),
    ) {
        item(key = "header") { ExerciseHeader(exercise, state.equipment[exercise.exerciseId]) }
        items(exercise.sets, key = { it.id }) { set ->
            Column(Modifier.animateItem(), verticalArrangement = Arrangement.spacedBy(d.stackTight)) {
                SetItem(exercise, set, set.id == current?.id, state, actions)
                val rest = state.rest
                if (rest != null && rest.afterSetId == set.id && current != null) {
                    RestLine(restText(rest, state.elapsedSeconds), Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

@Composable
private fun ExerciseHeader(exercise: ActiveExercise, equipment: Equipment?) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    Column(Modifier.padding(bottom = d.stackTight), verticalArrangement = Arrangement.spacedBy(d.hairline)) {
        Text(exercise.exerciseName, style = MaterialTheme.typography.headlineMedium)
        val line = joinDot(
            equipment?.let { stringResource(equipmentLabel(it)) },
            targetText(exercise.exerciseTrackingType, exercise.sets.size, exercise.plannedReps)
                ?: pluralStringResource(R.plurals.workout_sets, exercise.sets.size, exercise.sets.size),
            stringResource(R.string.workout_rest_target, clock(exercise.restSeconds ?: ActiveWorkoutViewModel.DEFAULT_REST_SECONDS)),
        )
        Text(line, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
    }
}

/**
 * One set: the current one is the card, the others rows. The item swaps
 * content in place when Done lands it (motion.md #2: one node, no
 * animateBounds). Swipe left deletes (rows: long-press too); on the card,
 * swipe right is Done.
 */
@Composable
private fun SetItem(
    exercise: ActiveExercise,
    set: WorkoutSet,
    isCurrent: Boolean,
    state: ActiveWorkoutUiState,
    actions: WorkoutActions,
) {
    val label = stringResource(R.string.workout_set_n, set.setNumber)
    AnimatedContent(
        targetState = isCurrent,
        transitionSpec = {
            fadeIn(motionTween(Durations.SET_LANDS / 2, Easings.Emphasized)) togetherWith
                fadeOut(motionTween(Durations.SET_LANDS / 2, Easings.Emphasized)) using
                SizeTransform(clip = false) { _, _ -> motionTween(Durations.SET_LANDS, Easings.Emphasized) }
        },
        label = "setLands",
    ) { cardShown ->
        SwipeableSet(
            onDelete = { actions.onDelete(exercise.id, set.id) },
            onDone = if (cardShown) actions.onDone else null,
        ) {
            if (cardShown) {
                CurrentSetCard(exercise, set, label, state, actions)
            } else {
                SetRow(
                    setLabel = label,
                    value = setValueText(exercise.exerciseTrackingType, set, state.preferredUnits),
                    state = if (set.isCompleted) SetState.Done else SetState.Upcoming,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeableSet(onDelete: () -> Unit, onDone: (() -> Unit)?, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val haptics = rememberHaptics()
    val swipe = rememberSwipeToDismissBoxState()
    LaunchedEffect(swipe.currentValue) {
        when (swipe.currentValue) {
            SwipeToDismissBoxValue.EndToStart -> { haptics(Haptic.SwipeThreshold); onDelete() }
            SwipeToDismissBoxValue.StartToEnd -> { onDone?.invoke(); swipe.reset() }
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }
    val deleteLabel = stringResource(R.string.workout_delete_set)
    val doneLabel = stringResource(R.string.workout_done_set_action)
    SwipeToDismissBox(
        state = swipe,
        enableDismissFromStartToEnd = onDone != null,
        // Long-press → Delete on rows only: on the card it would fire under a held − / +.
        modifier = Modifier
            .then(
                if (onDone == null) {
                    Modifier.combinedClickable(onClick = {}, onLongClick = { haptics(Haptic.DragStart); onDelete() }, onLongClickLabel = deleteLabel)
                } else Modifier,
            )
            .semantics {
                customActions = buildList {
                    if (onDone != null) add(CustomAccessibilityAction(doneLabel) { onDone(); true })
                    add(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
                }
            },
        backgroundContent = {
            val toDone = swipe.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Box(
                Modifier
                    .fillMaxSize()
                    .background(if (toDone) GymBuddyTheme.colors.limeTint else GymBuddyTheme.colors.dangerSwipe, MaterialTheme.shapes.medium)
                    .padding(horizontal = d.section),
                contentAlignment = if (toDone) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Icon(
                    painterResource(if (toDone) AppIcons.Ui.Check else AppIcons.Ui.Delete),
                    null,
                    Modifier.size(d.icon),
                    tint = if (toDone) cs.primary else cs.onSurface,
                )
            }
        },
    ) { content() }
}

@Composable
private fun CurrentSetCard(
    exercise: ActiveExercise,
    set: WorkoutSet,
    label: String,
    state: ActiveWorkoutUiState,
    actions: WorkoutActions,
) {
    val units = state.preferredUnits
    val editing = state.keypad?.takeIf { it.setId == set.id }?.field
    val panels: List<@Composable (Modifier) -> Unit> = SetFields.of(exercise.exerciseTrackingType).map { field ->
        { modifier ->
            val word = fieldWord(field)
            val number = panelNumber(set, field)
            ValuePanel(
                number = number,
                unit = unitText(field, units, number),
                editing = editing == field,
                onClick = { actions.onOpenKeypad(field) },
                onMinus = { actions.onStep(field, false) },
                onPlus = { actions.onStep(field, true) },
                modifier = modifier,
                step = stepText(field, units),
                minusEnabled = SetFields.canStep(set, field, plus = false),
                plusEnabled = SetFields.canStep(set, field, plus = true),
                minusDescription = stringResource(R.string.workout_cd_decrease, word),
                plusDescription = stringResource(R.string.workout_cd_increase, word),
            )
        }
    }
    SetCard(
        title = label,
        panels = panels,
        hint = if (state.showHint) stringResource(R.string.workout_hint) else null,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun UpNextRow(state: ActiveWorkoutUiState, onClick: () -> Unit) {
    // Nothing after the last exercise: Done already says "finish workout".
    val next = state.exercises.getOrNull(state.currentExerciseIndex + 1) ?: return
    InfoRow(
        label = stringResource(R.string.workout_up_next),
        value = next.exerciseName,
        onClick = onClick,
    )
}

@Composable
private fun doneLabel(state: ActiveWorkoutUiState): String {
    val set = state.currentExercise?.currentSet
    return when {
        set == null -> stringResource(R.string.workout_next_exercise)
        state.lastSetLeft -> stringResource(R.string.workout_done_finish)
        else -> stringResource(R.string.workout_done_set, set.setNumber)
    }
}

/** Titleless keypad sheet; the label names the set and value (Figma 53:2686). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutKeypadSheet(
    keypad: WorkoutKeypad,
    state: ActiveWorkoutUiState,
    next: SetField?,
    onInput: (KeypadInput) -> Unit,
) {
    val set = state.exercises.firstOrNull { it.id == keypad.exerciseId }?.sets?.firstOrNull { it.id == keypad.setId } ?: return
    val units = state.preferredUnits
    val rule = keypad.rule
    AppBottomSheet(onDismiss = { onInput(KeypadInput.Cancel) }, title = null) {
        Keypad(
            label = stringResource(R.string.workout_keypad_label, set.setNumber, fieldWord(keypad.field)),
            value = keypadDisplay(keypad.field, keypad.text),
            unit = unitText(keypad.field, units, keypad.text),
            onInput = onInput,
            decimalEnabled = keypad.field.kind == KeypadValue.Kind.Decimal,
            nextEnabled = rule == null,
            nextLabel = next?.let { stringResource(R.string.keypad_next_field, fieldWord(it)) }
                ?: stringResource(R.string.workout_keypad_save),
            message = rule?.let { ruleText(it) },
        )
    }
}

@Composable
private fun FinishDialog(
    title: String,
    preview: FinishPreview,
    units: PreferredUnits,
    onFinish: () -> Unit,
    onKeepGoing: () -> Unit,
) {
    val numbers = joinDot(
        stringResource(R.string.workout_minutes, preview.minutes),
        pluralStringResource(R.plurals.workout_sets, preview.sets, preview.sets),
        if (preview.lifted > 0f) "${wholeNumber(preview.lifted)} ${unitText(SetField.Weight, units)}" else null,
    )
    val left = if (preview.setsLeft > 0) pluralStringResource(R.plurals.workout_sets_left, preview.setsLeft, preview.setsLeft) else null
    AppDialog(
        title = stringResource(R.string.workout_finish_title, title),
        body = listOfNotNull(numbers, left).joinToString("\n\n"),
        confirmLabel = stringResource(R.string.workout_finish),
        onConfirm = onFinish,
        dismissLabel = stringResource(R.string.workout_keep_going),
        onDismiss = onKeepGoing,
    )
}
