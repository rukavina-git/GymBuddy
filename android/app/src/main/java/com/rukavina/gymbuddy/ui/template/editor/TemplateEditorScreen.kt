package com.rukavina.gymbuddy.ui.template.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.ui.design.AppBottomSheet
import com.rukavina.gymbuddy.ui.design.AppChip
import com.rukavina.gymbuddy.ui.design.AppDialog
import com.rukavina.gymbuddy.ui.design.AppSnackbar
import com.rukavina.gymbuddy.ui.design.AppTextField
import com.rukavina.gymbuddy.ui.design.BackTopBar
import com.rukavina.gymbuddy.ui.design.ButtonSize
import com.rukavina.gymbuddy.ui.design.Keypad
import com.rukavina.gymbuddy.ui.design.KeypadInput
import com.rukavina.gymbuddy.ui.design.KeypadValue
import com.rukavina.gymbuddy.ui.design.OutlineButton
import com.rukavina.gymbuddy.ui.design.PrimaryButton
import com.rukavina.gymbuddy.ui.design.SecondaryButton
import com.rukavina.gymbuddy.ui.design.keypadSeparator
import com.rukavina.gymbuddy.ui.template.clock
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.theme.Haptic
import com.rukavina.gymbuddy.ui.theme.rememberHaptics
import com.rukavina.gymbuddy.utils.UnitConverter
import kotlinx.coroutines.launch

/**
 * Template editor (Figma Editor A 93:12841 and friends). × and lime
 * Save, name first, one card per exercise (chips → keypad, quiet rest,
 * note), swipe to remove with Undo, drag handle + TalkBack Move up/down.
 * [picked]: ids handed back by the picker, added once and cleared with
 * [onPickedHandled].
 */
@Composable
fun TemplateEditorScreen(
    onClose: () -> Unit,
    onAddExercises: (inTemplateIds: List<String>) -> Unit,
    picked: List<String>?,
    onPickedHandled: () -> Unit,
    viewModel: TemplateEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(picked) {
        if (picked != null) {
            viewModel.onPicked(picked)
            onPickedHandled()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                TemplateEditorEvent.Close -> onClose()
                TemplateEditorEvent.SaveFailed -> scope.launch {
                    snackbar.showSnackbar(context.getString(R.string.editor_save_failed))
                }
                is TemplateEditorEvent.Added -> scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(context.resources.getQuantityString(R.plurals.editor_added, event.count, event.count))
                }
                is TemplateEditorEvent.Removed -> scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    val result = snackbar.showSnackbar(
                        message = context.getString(R.string.editor_removed, event.name),
                        actionLabel = context.getString(R.string.undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove(event.key)
                }
            }
        }
    }
    BackHandler { viewModel.requestClose() }

    val separator = keypadSeparator()
    TemplateEditorContent(
        state = state,
        actions = EditorActions(
            onClose = viewModel::requestClose,
            onSave = viewModel::save,
            onName = viewModel::setName,
            onNote = viewModel::setNote,
            onOpenKeypad = viewModel::openKeypad,
            onRemove = viewModel::remove,
            onMove = viewModel::move,
            onMoveBy = viewModel::moveBy,
            onAddExercises = {
                viewModel.startSwap(null)
                onAddExercises(state.exercises.map { it.exerciseId })
            },
            onSwap = { key ->
                viewModel.startSwap(key)
                onAddExercises(state.exercises.map { it.exerciseId })
            },
            onRestore = viewModel::restore,
        ),
        snackbar = snackbar,
    )

    state.keypad?.let { keypad ->
        EditorKeypadSheet(
            keypad = keypad,
            state = state,
            next = viewModel.nextTarget(keypad.target),
            onInput = { viewModel.keypad(it, separator) },
        )
    }
    if (state.confirmDiscard) {
        AppDialog(
            title = stringResource(R.string.editor_discard_title),
            body = if (state.isNew || state.name.isBlank()) stringResource(R.string.editor_discard_body_new)
            else stringResource(R.string.editor_discard_body, state.name.trim()),
            confirmLabel = stringResource(R.string.editor_discard),
            onConfirm = viewModel::discard,
            dismissLabel = stringResource(R.string.editor_keep_editing),
            onDismiss = viewModel::keepEditing,
            destructive = true,
        )
    }
}

internal data class EditorActions(
    val onClose: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onName: (String) -> Unit = {},
    val onNote: (Long, String) -> Unit = { _, _ -> },
    val onOpenKeypad: (KeypadTarget) -> Unit = {},
    val onRemove: (Long) -> Unit = {},
    val onMove: (Long, Int) -> Unit = { _, _ -> },
    val onMoveBy: (Long, Int) -> Unit = { _, _ -> },
    val onAddExercises: () -> Unit = {},
    val onSwap: (Long) -> Unit = {},
    val onRestore: (Long) -> Unit = {},
)

@Composable
internal fun TemplateEditorContent(
    state: TemplateEditorUiState,
    actions: EditorActions,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val haptics = rememberHaptics()
    val listState = rememberLazyListState()
    val gapPx = with(LocalDensity.current) { d.stackTight.toPx() }
    var noteKey by rememberSaveable { mutableStateOf<Long?>(null) }
    var dragKey by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val exercises by rememberUpdatedState(state.exercises)

    // Drag: swap with the card under the dragged card's centre; keep the
    // finger on the card by shifting the offset by the card it passed.
    val onDrag: (Long, Float) -> Unit = drag@{ key, dy ->
        dragOffset += dy
        val visible = listState.layoutInfo.visibleItemsInfo
        val current = visible.firstOrNull { it.key == key } ?: return@drag
        val centre = current.offset + dragOffset + current.size / 2f
        val target = visible.firstOrNull { info ->
            info.key is Long && info.key != key && centre >= info.offset && centre <= info.offset + info.size
        } ?: return@drag
        val toIndex = exercises.indexOfFirst { it.key == target.key }
        if (toIndex < 0) return@drag
        val down = target.index > current.index
        actions.onMove(key, toIndex)
        dragOffset += if (down) -(target.size + gapPx) else (target.size + gapPx)
    }

    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().imePadding()) {
                BackTopBar(
                    onBack = actions.onClose,
                    close = true,
                    title = stringResource(if (state.isNew) R.string.editor_title_new else R.string.editor_title_edit),
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.editor_save),
                        onClick = actions.onSave,
                        size = ButtonSize.Small,
                        enabled = state.canSave,
                        loading = state.saving,
                        modifier = Modifier.padding(end = d.stackTight),
                    )
                }
                if (!state.loaded) return@Column
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = d.insetScreen, end = d.insetScreen, bottom = d.section),
                    verticalArrangement = Arrangement.spacedBy(d.stackTight),
                ) {
                    item(key = "name") {
                        NameField(state.name, state.nameError, actions.onName, Modifier.padding(bottom = d.group))
                    }
                    if (state.exercises.isEmpty()) {
                        item(key = "hint") {
                            Text(
                                stringResource(R.string.editor_empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = d.hairline, vertical = d.stack),
                            )
                        }
                    }
                    items(state.exercises, key = { it.key }) { exercise ->
                        val index = state.exercises.indexOf(exercise)
                        val dragging = dragKey == exercise.key
                        if (exercise.deleted) {
                            DeletedCard(exercise, actions, Modifier.animateItem())
                            return@items
                        }
                        ExerciseCard(
                            exercise = exercise,
                            units = state.units,
                            selected = state.keypad?.target?.takeIf { it.key == exercise.key }?.field,
                            canMoveUp = index > 0,
                            canMoveDown = index < state.exercises.lastIndex,
                            editingNote = noteKey == exercise.key,
                            onEditNote = { noteKey = if (it) exercise.key else null },
                            actions = actions,
                            onDragStart = { haptics(Haptic.DragStart); dragKey = exercise.key; dragOffset = 0f },
                            onDrag = { dy -> onDrag(exercise.key, dy) },
                            onDragEnd = { dragKey = null; dragOffset = 0f },
                            modifier = Modifier
                                .then(if (dragging) Modifier else Modifier.animateItem())
                                .zIndex(if (dragging) 1f else 0f)
                                .graphicsLayer { translationY = if (dragging) dragOffset else 0f },
                        )
                    }
                    item(key = "add") {
                        OutlineButton(
                            text = stringResource(R.string.editor_add_exercise),
                            onClick = actions.onAddExercises,
                            icon = AppIcons.Ui.Add,
                            modifier = Modifier.fillMaxWidth().padding(top = d.stackTight),
                        )
                    }
                }
            }
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).padding(d.insetScreen),
            ) { AppSnackbar(it) }
        }
    }
}

/** Label, big name text, underline (Editor A). Error: red line + "Give it a name". */
@Composable
private fun NameField(value: String, error: Boolean, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val c = GymBuddyTheme.colors
    val d = GymBuddyTheme.dimens
    val type = MaterialTheme.typography
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val accent = if (error) cs.error else cs.onSurfaceVariant
    val errorText = stringResource(R.string.editor_name_error)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = type.headlineMedium.copy(color = cs.onSurface),
        cursorBrush = SolidColor(c.cobalt),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = d.hairline)
            .semantics { if (error) error(errorText) },
        decorationBox = { inner ->
            Column(verticalArrangement = Arrangement.spacedBy(d.hairline)) {
                Text(stringResource(R.string.editor_name), style = type.labelMedium, color = accent)
                Box(Modifier.padding(vertical = d.hairline)) {
                    if (value.isEmpty()) {
                        Text(stringResource(R.string.editor_name_placeholder), style = type.headlineMedium, color = cs.onSurfaceVariant)
                    }
                    inner()
                }
                val lineColor = when {
                    error -> cs.error
                    focused -> c.cobalt
                    else -> cs.outlineVariant
                }
                Box(
                    Modifier.fillMaxWidth()
                        .height(if (error || focused) d.strokeEmphasis else d.strokeDivider)
                        .background(lineColor),
                )
                if (error) Text(errorText, style = type.bodySmall, color = cs.error)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ExerciseCard(
    exercise: EditorExercise,
    units: PreferredUnits,
    selected: EditField?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    editingNote: Boolean,
    onEditNote: (Boolean) -> Unit,
    actions: EditorActions,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val c = GymBuddyTheme.colors
    val d = GymBuddyTheme.dimens
    val haptics = rememberHaptics()
    val swipe = rememberSwipeToDismissBoxState()
    LaunchedEffect(swipe.currentValue) {
        if (swipe.currentValue != SwipeToDismissBoxValue.Settled) {
            haptics(Haptic.SwipeThreshold)
            actions.onRemove(exercise.key)
        }
    }
    val moveUp = stringResource(R.string.a11y_move_up)
    val moveDown = stringResource(R.string.a11y_move_down)
    val remove = stringResource(R.string.a11y_remove)
    SwipeToDismissBox(
        state = swipe,
        enableDismissFromStartToEnd = false,
        modifier = modifier.semantics {
            customActions = buildList {
                if (canMoveUp) add(CustomAccessibilityAction(moveUp) { actions.onMoveBy(exercise.key, -1); true })
                if (canMoveDown) add(CustomAccessibilityAction(moveDown) { actions.onMoveBy(exercise.key, 1); true })
                add(CustomAccessibilityAction(remove) { actions.onRemove(exercise.key); true })
            }
        },
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(c.dangerSwipe, MaterialTheme.shapes.medium).padding(horizontal = d.section),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(painterResource(AppIcons.Ui.Delete), null, Modifier.size(d.icon), tint = cs.onSurface)
            }
        },
    ) {
        Surface(shape = MaterialTheme.shapes.medium, color = cs.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(start = d.insetCard, end = d.hairline, bottom = d.stack),
                verticalArrangement = Arrangement.spacedBy(d.stackTight),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(exercise.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).padding(top = d.stack))
                    Box(
                        Modifier
                            .minimumInteractiveComponentSize()
                            .pointerInput(exercise.key) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { onDragStart() },
                                    onDrag = { change, amount -> change.consume(); onDrag(amount.y) },
                                    onDragEnd = onDragEnd,
                                    onDragCancel = onDragEnd,
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(AppIcons.Ui.Drag),
                            stringResource(R.string.cd_reorder, exercise.name),
                            Modifier.size(d.icon),
                            tint = cs.onSurfaceVariant,
                        )
                    }
                }
                Row(Modifier.padding(end = d.stack), verticalAlignment = Alignment.CenterVertically) {
                    FlowRow(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(d.hairline),
                    ) {
                        chipFields(exercise.trackingType).forEach { field ->
                            AppChip(
                                label = chipLabel(exercise, field, units),
                                selected = selected == field,
                                onClick = { actions.onOpenKeypad(KeypadTarget(exercise.key, field)) },
                            )
                        }
                    }
                    RestValue(exercise.restSeconds, selected == EditField.Rest) {
                        actions.onOpenKeypad(KeypadTarget(exercise.key, EditField.Rest))
                    }
                }
                NoteLine(exercise, editingNote, onEditNote, actions.onNote)
            }
        }
    }
}

/**
 * A deleted exercise (Figma R13-12): muted name, "Deleted exercise ·
 * still starts with this template", Swap / Remove / Restore.
 */
@Composable
private fun DeletedCard(exercise: EditorExercise, actions: EditorActions, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    Surface(shape = MaterialTheme.shapes.medium, color = cs.surfaceContainer, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(d.insetCard), verticalArrangement = Arrangement.spacedBy(d.hairline)) {
            Text(exercise.name, style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant)
            Text(stringResource(R.string.editor_deleted), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            Row(Modifier.padding(top = d.stackTight), horizontalArrangement = Arrangement.spacedBy(d.stackTight)) {
                SecondaryButton(stringResource(R.string.editor_swap), { actions.onSwap(exercise.key) }, size = ButtonSize.Small)
                SecondaryButton(stringResource(R.string.editor_remove), { actions.onRemove(exercise.key) }, size = ButtonSize.Small)
                SecondaryButton(stringResource(R.string.editor_restore), { actions.onRestore(exercise.key) }, size = ButtonSize.Small)
            }
        }
    }
}

/** Timer + value, quieter than the chips (owner: rest shown, but quiet). */
@Composable
private fun RestValue(seconds: Int?, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val text = seconds?.let(::clock) ?: stringResource(R.string.rest_none)
    val description = stringResource(R.string.cd_rest, text)
    Row(
        Modifier
            .minimumInteractiveComponentSize()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = d.hairline),
        horizontalArrangement = Arrangement.spacedBy(d.hairline),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (selected) GymBuddyTheme.colors.cobalt else cs.onSurfaceVariant
        Icon(painterResource(AppIcons.Ui.Timer), null, Modifier.size(d.iconSmall), tint = tint)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = tint)
    }
}

/** "+ Add note" when empty, the note text when filled; tap edits in place. */
@Composable
private fun NoteLine(
    exercise: EditorExercise,
    editing: Boolean,
    onEdit: (Boolean) -> Unit,
    onNote: (Long, String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    if (editing) {
        AppTextField(
            value = exercise.notes.orEmpty(),
            onValueChange = { onNote(exercise.key, it) },
            label = stringResource(R.string.editor_note),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onEdit(false) }),
            modifier = Modifier.padding(end = d.stack),
        )
        return
    }
    val note = exercise.notes?.takeIf { it.isNotBlank() }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { onEdit(true) }
            .padding(vertical = d.hairline),
        horizontalArrangement = Arrangement.spacedBy(d.hairline),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (note == null) {
            Icon(painterResource(AppIcons.Ui.Add), null, Modifier.size(GymBuddyTheme.dimens.iconSmall), tint = cs.onSurfaceVariant)
            Text(stringResource(R.string.editor_add_note), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        } else {
            Text(note, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun chipLabel(e: EditorExercise, field: EditField, units: PreferredUnits): String = when (field) {
    EditField.Sets -> pluralStringResource(R.plurals.set_count, e.sets, e.sets)
    EditField.Reps -> e.reps?.let { stringResource(R.string.chip_reps, it) } ?: stringResource(R.string.chip_reps_empty)
    EditField.Duration -> e.durationSeconds?.let(::clock) ?: stringResource(R.string.chip_time_empty)
    EditField.Distance -> e.distanceMeters?.let { stringResource(R.string.distance_m, it.toInt()) }
        ?: stringResource(R.string.chip_distance_empty)
    EditField.Weight -> {
        val unit = UnitConverter.getWeightUnitLabel(units)
        e.weightKg?.let { stringResource(R.string.weight_value, UnitConverter.weightToDisplayUnit(it, units), unit) }
            ?: stringResource(R.string.chip_weight_empty, unit)
    }
    EditField.Rest -> e.restSeconds?.let(::clock) ?: stringResource(R.string.rest_none)
}

/** Word for a field: keypad unit, label and "Next: …". */
@Composable
internal fun fieldWord(field: EditField, units: PreferredUnits): String = when (field) {
    EditField.Sets -> stringResource(R.string.field_sets)
    EditField.Reps -> stringResource(R.string.field_reps)
    EditField.Duration -> stringResource(R.string.field_time)
    EditField.Distance -> stringResource(R.string.field_distance)
    EditField.Weight -> UnitConverter.getWeightUnitLabel(units)
    EditField.Rest -> stringResource(R.string.field_rest)
}

/** Keypad sheet, no title: the keypad's label names the exercise and field (R12-11). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorKeypadSheet(
    keypad: KeypadState,
    state: TemplateEditorUiState,
    next: KeypadTarget?,
    onInput: (KeypadInput) -> Unit,
) {
    val exercise = state.exercises.firstOrNull { it.key == keypad.target.key } ?: return
    val field = keypad.target.field
    val word = fieldWord(field, state.units)
    val clockField = field.kind == KeypadValue.Kind.Clock
    AppBottomSheet(onDismiss = { onInput(KeypadInput.Cancel) }, title = null) {
        Keypad(
            label = stringResource(R.string.keypad_label, exercise.name, word),
            value = if (clockField) KeypadValue.clockText(keypad.text) else keypad.text,
            unit = if (clockField && field == EditField.Duration) "" else word,
            onInput = onInput,
            decimalEnabled = field.kind == KeypadValue.Kind.Decimal,
            nextLabel = next?.let { stringResource(R.string.keypad_next_field, fieldWord(it.field, state.units)) }
                ?: stringResource(R.string.keypad_done),
        )
    }
}
