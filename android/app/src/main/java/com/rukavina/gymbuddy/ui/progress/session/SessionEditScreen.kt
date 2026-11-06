package com.rukavina.gymbuddy.ui.progress.session

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.ui.design.AppBottomSheet
import com.rukavina.gymbuddy.ui.design.AppChip
import com.rukavina.gymbuddy.ui.design.AppDialog
import com.rukavina.gymbuddy.ui.design.AppLinkButton
import com.rukavina.gymbuddy.ui.design.AppSnackbar
import com.rukavina.gymbuddy.ui.design.AppTextField
import com.rukavina.gymbuddy.ui.design.BackTopBar
import com.rukavina.gymbuddy.ui.design.ButtonSize
import com.rukavina.gymbuddy.ui.design.Keypad
import com.rukavina.gymbuddy.ui.design.KeypadInput
import com.rukavina.gymbuddy.ui.design.KeypadValue
import com.rukavina.gymbuddy.ui.design.PrimaryButton
import com.rukavina.gymbuddy.ui.design.keypadSeparator
import com.rukavina.gymbuddy.ui.template.editor.NameField
import com.rukavina.gymbuddy.ui.template.joinDot
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.Durations
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.theme.Haptic
import com.rukavina.gymbuddy.ui.theme.motionTween
import com.rukavina.gymbuddy.ui.theme.rememberHaptics
import com.rukavina.gymbuddy.ui.workout.SetField
import com.rukavina.gymbuddy.ui.workout.SetFields
import com.rukavina.gymbuddy.ui.workout.WorkoutSet
import com.rukavina.gymbuddy.ui.workout.active.fieldWord
import com.rukavina.gymbuddy.ui.workout.active.keypadDisplay
import com.rukavina.gymbuddy.ui.workout.active.panelNumber
import com.rukavina.gymbuddy.ui.workout.active.ruleText
import com.rukavina.gymbuddy.ui.workout.active.setValueText
import com.rukavina.gymbuddy.ui.workout.active.unitText
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Edit workout (Figma R14-4 107:16270): × + Save, name, date + start →
 * end, note, the detail's exercise cards. An open card's set chips open
 * the workout keypad; swipe a set to delete it (Undo); "+ Add set".
 */
@Composable
fun SessionEditScreen(
    onClose: () -> Unit,
    viewModel: SessionEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SessionEditEvent.Close -> onClose()
                SessionEditEvent.SaveFailed -> scope.launch {
                    snackbar.showSnackbar(context.getString(R.string.editor_save_failed))
                }
                is SessionEditEvent.SetDeleted -> scope.launch {
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
    BackHandler { viewModel.requestClose() }

    val separator = keypadSeparator()
    SessionEditContent(
        state = state,
        actions = SessionEditActions(
            onClose = viewModel::requestClose,
            onSave = viewModel::save,
            onTitle = viewModel::setTitle,
            onNote = viewModel::setNote,
            onDate = viewModel::setDate,
            onStart = viewModel::setStart,
            onEnd = viewModel::setEnd,
            onOpenKeypad = viewModel::openKeypad,
            onDeleteSet = viewModel::deleteSet,
            onAddSet = viewModel::addSet,
        ),
        snackbar = snackbar,
    )

    state.keypad?.let { keypad ->
        EditKeypadSheet(keypad, state, viewModel.nextTarget(keypad)) { viewModel.keypad(it, separator) }
    }
    if (state.confirmDiscard) {
        AppDialog(
            title = stringResource(R.string.editor_discard_title),
            body = stringResource(R.string.editor_discard_body, state.title.trim().ifEmpty { stringResource(R.string.session_edit_title) }),
            confirmLabel = stringResource(R.string.editor_discard),
            onConfirm = viewModel::discard,
            dismissLabel = stringResource(R.string.editor_keep_editing),
            onDismiss = viewModel::keepEditing,
            destructive = true,
        )
    }
}

internal data class SessionEditActions(
    val onClose: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onTitle: (String) -> Unit = {},
    val onNote: (String) -> Unit = {},
    val onDate: (LocalDate) -> Unit = {},
    val onStart: (LocalTime) -> Unit = {},
    val onEnd: (LocalTime) -> Unit = {},
    val onOpenKeypad: (exerciseId: String, setId: String, SetField) -> Unit = { _, _, _ -> },
    val onDeleteSet: (exerciseId: String, setId: String) -> Unit = { _, _ -> },
    val onAddSet: (exerciseId: String) -> Unit = {},
)

private enum class Picker { Date, Start, End }

@Composable
internal fun SessionEditContent(
    state: SessionEditUiState,
    actions: SessionEditActions,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    var open by rememberSaveable { mutableStateOf(ArrayList<String>()) }
    var editingNote by rememberSaveable { mutableStateOf(false) }
    var picker by rememberSaveable { mutableStateOf<Picker?>(null) }

    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().imePadding()) {
                BackTopBar(onBack = actions.onClose, close = true, title = stringResource(R.string.session_edit_title)) {
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
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = d.insetScreen, end = d.insetScreen, bottom = d.section),
                    verticalArrangement = Arrangement.spacedBy(d.stackTight),
                ) {
                    item(key = "name") {
                        NameField(state.title, state.nameError, actions.onTitle, Modifier.padding(bottom = d.stack))
                    }
                    item(key = "when") { WhenRow(state) { picker = it } }
                    item(key = "note") {
                        NoteRow(state.note, editingNote, { editingNote = it }, actions.onNote, Modifier.padding(bottom = d.stack))
                    }
                    items(state.exercises, key = { it.id }) { exercise ->
                        val id = exercise.id
                        EditCard(
                            exercise = exercise,
                            units = state.units,
                            expanded = id in open,
                            selected = state.keypad?.takeIf { it.exerciseId == id },
                            onToggle = { open = if (id in open) ArrayList(open - id) else ArrayList(open + id) },
                            actions = actions,
                        )
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(d.insetScreen)) { AppSnackbar(it) }
        }
    }

    when (picker) {
        Picker.Date -> DateDialog(state, { picker = null }) { actions.onDate(it); picker = null }
        Picker.Start -> TimeDialog(state.startedAt, state) { time -> time?.let(actions.onStart); picker = null }
        Picker.End -> TimeDialog(state.endedAt, state) { time -> time?.let(actions.onEnd); picker = null }
        null -> Unit
    }
}

/** "Mon 26 Oct" and "18:24 → 19:16" (R14-4); each value opens its picker. */
@Composable
private fun WhenRow(state: SessionEditUiState, onPick: (Picker) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val locale = Locale.getDefault()
    val start = Instant.ofEpochMilli(state.startedAt).atZone(state.zone)
    val end = Instant.ofEpochMilli(state.endedAt).atZone(state.zone)
    val day = DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEdMMM"), locale).format(start)
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    val startText = start.format(time)
    val endText = end.format(time)
    val dateCd = stringResource(R.string.session_edit_date, day)
    val startCd = stringResource(R.string.session_edit_start, startText)
    val endCd = stringResource(R.string.session_edit_end, endText)
    Surface(shape = MaterialTheme.shapes.medium, color = cs.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.heightIn(min = d.target).padding(horizontal = d.stackTight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PickText(day, dateCd, MaterialTheme.typography.bodyLarge, Modifier.weight(1f)) { onPick(Picker.Date) }
            PickText(startText, startCd, MaterialTheme.typography.titleSmall) { onPick(Picker.Start) }
            Text(stringResource(R.string.session_edit_arrow), style = MaterialTheme.typography.titleSmall)
            PickText(endText, endCd, MaterialTheme.typography.titleSmall) { onPick(Picker.End) }
        }
    }
}

@Composable
private fun PickText(
    text: String,
    description: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val d = GymBuddyTheme.dimens
    Box(
        modifier
            .heightIn(min = d.target)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = d.stackTight),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, style = style)
    }
}

/** Note card (R14-4): icon + note, or the placeholder; tap edits in place. */
@Composable
private fun NoteRow(
    note: String,
    editing: Boolean,
    onEdit: (Boolean) -> Unit,
    onNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    if (editing) {
        AppTextField(
            value = note,
            onValueChange = onNote,
            label = stringResource(R.string.workout_note_label),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onEdit(false) }),
            modifier = modifier.fillMaxWidth(),
        )
        return
    }
    Surface(shape = MaterialTheme.shapes.medium, color = cs.surfaceContainer, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier
                .heightIn(min = d.target)
                .clickable(role = Role.Button) { onEdit(true) }
                .padding(horizontal = d.insetCard, vertical = d.stack),
            horizontalArrangement = Arrangement.spacedBy(d.stackTight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(AppIcons.Ui.Note), null, Modifier.size(d.icon), tint = cs.onSurfaceVariant)
            if (note.isBlank()) {
                Text(stringResource(R.string.workout_note_placeholder), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            } else {
                Text(note, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** "3 sets · top 100 kg × 5"; without weight "best …" (09 decision 4, from the edited sets). */
@Composable
private fun editLine(type: ExerciseTrackingType, sets: List<WorkoutSet>, units: PreferredUnits): String {
    val count = pluralStringResource(R.plurals.workout_sets, sets.size, sets.size)
    val fields = SetFields.of(type)
    val top = sets.maxWithOrNull(
        compareBy<WorkoutSet>({ KeypadValue.parse(it.weight) ?: 0f })
            .thenBy { SetFields.value(it, fields.last()) ?: 0f },
    ) ?: return count
    val value = setValueText(type, top, units)
    val weighted = SetField.Weight in fields && (KeypadValue.parse(top.weight) ?: 0f) > 0f
    return joinDot(count, stringResource(if (weighted) R.string.summary_top else R.string.summary_best, value))
}

@Composable
private fun EditCard(
    exercise: EditExercise,
    units: PreferredUnits,
    expanded: Boolean,
    selected: EditKeypad?,
    onToggle: () -> Unit,
    actions: SessionEditActions,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val type = exercise.performed.exerciseTrackingType
    val state = stringResource(if (expanded) R.string.state_expanded else R.string.state_collapsed)
    Surface(shape = MaterialTheme.shapes.medium, color = cs.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = d.target)
                    .clickable(role = Role.Button, onClick = onToggle)
                    .semantics(mergeDescendants = true) { stateDescription = state }
                    .padding(horizontal = d.insetCard, vertical = d.stack),
                horizontalArrangement = Arrangement.spacedBy(d.stackTight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.hairline)) {
                    Text(exercise.performed.exerciseName, style = MaterialTheme.typography.titleSmall)
                    if (!expanded) {
                        Text(editLine(type, exercise.sets, units), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                }
                Icon(
                    painterResource(if (expanded) AppIcons.Ui.ChevronUp else AppIcons.Ui.ChevronDown),
                    contentDescription = null,
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(d.iconSmall),
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(motionTween(Durations.MEDIUM2)) + fadeIn(motionTween(Durations.MEDIUM2)),
                exit = shrinkVertically(motionTween(Durations.SHORT4)) + fadeOut(motionTween(Durations.SHORT4)),
            ) {
                Column(
                    Modifier.padding(bottom = d.stackTight),
                    verticalArrangement = Arrangement.spacedBy(d.hairline),
                ) {
                    exercise.sets.forEach { set ->
                        key(set.id) {
                            SetRow(exercise, set, units, selected?.takeIf { it.setId == set.id }?.field, actions)
                        }
                    }
                    AppLinkButton(
                        stringResource(R.string.session_add_set),
                        { actions.onAddSet(exercise.id) },
                        Modifier.padding(start = d.hairline),
                    )
                }
            }
        }
    }
}

/** "1  [80 kg] [8 reps]"; swipe left deletes (Undo). */
@Composable
private fun SetRow(
    exercise: EditExercise,
    set: WorkoutSet,
    units: PreferredUnits,
    selected: SetField?,
    actions: SessionEditActions,
) {
    val cs = MaterialTheme.colorScheme
    val c = GymBuddyTheme.colors
    val d = GymBuddyTheme.dimens
    val haptics = rememberHaptics()
    val swipe = rememberSwipeToDismissBoxState()
    LaunchedEffect(swipe.currentValue) {
        if (swipe.currentValue != SwipeToDismissBoxValue.Settled) {
            haptics(Haptic.SwipeThreshold)
            actions.onDeleteSet(exercise.id, set.id)
        }
    }
    val number = stringResource(R.string.session_set_number, set.setNumber)
    val remove = stringResource(R.string.a11y_remove)
    SwipeToDismissBox(
        state = swipe,
        enableDismissFromStartToEnd = false,
        modifier = Modifier.semantics {
            customActions = listOf(CustomAccessibilityAction(remove) { actions.onDeleteSet(exercise.id, set.id); true })
        },
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(c.dangerSwipe).padding(horizontal = d.section),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(painterResource(AppIcons.Ui.Delete), null, Modifier.size(d.icon), tint = cs.onSurface)
            }
        },
    ) {
        Row(
            Modifier.fillMaxWidth().background(cs.surfaceContainer).padding(horizontal = d.insetCard),
            horizontalArrangement = Arrangement.spacedBy(d.stackTight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${set.setNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                modifier = Modifier.widthIn(min = d.stack).semantics { contentDescription = number },
            )
            SetFields.of(exercise.performed.exerciseTrackingType).forEach { field ->
                val value = SetFields.text(set, field)
                val label = if (value.isEmpty()) unitText(field, units).ifEmpty { fieldWord(field) }
                else listOf(panelNumber(set, field), unitText(field, units, value)).filter { it.isNotEmpty() }.joinToString(" ")
                AppChip(label = label, selected = selected == field, onClick = { actions.onOpenKeypad(exercise.id, set.id, field) })
            }
        }
    }
}

/** Titleless keypad sheet; the label names the set and value (as in the workout). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditKeypadSheet(
    keypad: EditKeypad,
    state: SessionEditUiState,
    next: Pair<String, SetField>?,
    onInput: (KeypadInput) -> Unit,
) {
    val set = state.exercises.firstOrNull { it.id == keypad.exerciseId }?.sets?.firstOrNull { it.id == keypad.setId } ?: return
    val rule = keypad.rule
    AppBottomSheet(onDismiss = { onInput(KeypadInput.Cancel) }, title = null) {
        Keypad(
            label = stringResource(R.string.workout_keypad_label, set.setNumber, fieldWord(keypad.field)),
            value = keypadDisplay(keypad.field, keypad.text),
            unit = unitText(keypad.field, state.units, keypad.text),
            onInput = onInput,
            decimalEnabled = keypad.field.kind == KeypadValue.Kind.Decimal,
            nextEnabled = rule == null,
            nextLabel = next?.let { stringResource(R.string.keypad_next_field, fieldWord(it.second)) }
                ?: stringResource(R.string.keypad_done),
            message = rule?.let { ruleText(it) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(state: SessionEditUiState, onDismiss: () -> Unit, onPicked: (LocalDate) -> Unit) {
    val day = Instant.ofEpochMilli(state.startedAt).atZone(state.zone).toLocalDate()
    // DatePicker works in UTC midnight millis.
    val picker = rememberDatePickerState(initialSelectedDateMillis = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AppLinkButton(stringResource(R.string.session_edit_ok), {
                val millis = picker.selectedDateMillis
                if (millis == null) onDismiss() else onPicked(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
            })
        },
        dismissButton = { AppLinkButton(stringResource(R.string.cancel), onDismiss) },
    ) {
        DatePicker(picker)
    }
}

/** [onDone] null = cancelled. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(millis: Long, state: SessionEditUiState, onDone: (LocalTime?) -> Unit) {
    val d = GymBuddyTheme.dimens
    val time = Instant.ofEpochMilli(millis).atZone(state.zone).toLocalTime()
    val context = LocalContext.current
    val picker = rememberTimePickerState(
        initialHour = time.hour,
        initialMinute = time.minute,
        is24Hour = android.text.format.DateFormat.is24HourFormat(context),
    )
    BasicAlertDialog(onDismissRequest = { onDone(null) }) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(d.insetDialog), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(picker)
                Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(d.hairline)) {
                    AppLinkButton(stringResource(R.string.cancel), { onDone(null) })
                    AppLinkButton(stringResource(R.string.session_edit_ok), { onDone(LocalTime.of(picker.hour, picker.minute)) })
                }
            }
        }
    }
}
