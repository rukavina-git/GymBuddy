package com.rukavina.gymbuddy.ui.workout.active

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.design.AppBottomSheet
import com.rukavina.gymbuddy.ui.design.AppIconButton
import com.rukavina.gymbuddy.ui.design.AppSwitch
import com.rukavina.gymbuddy.ui.design.AppTextField
import com.rukavina.gymbuddy.ui.design.Keypad
import com.rukavina.gymbuddy.ui.design.KeypadInput
import com.rukavina.gymbuddy.ui.design.KeypadValue
import com.rukavina.gymbuddy.ui.design.OutlineButton
import com.rukavina.gymbuddy.ui.design.PrimaryButton
import com.rukavina.gymbuddy.ui.template.joinDot
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.FullShape
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.workout.ActiveExercise

/** Done mark in a sheet row, same as the set row's (DESIGN.md §4 raw size, owed). */
private val Mark = 28.dp

/** Which workout sheet is open (state lives in the screen, saveable). */
internal enum class WorkoutSheet { Exercises, Reorder, Note, Rest, YourNote }

/**
 * All exercises (Figma 53:3272): title, "15 exercises · 2 done", one row
 * per exercise (done mark, name, done/total sets); tap shows it. [reorder]
 * swaps the counts for Move up / Move down, like the 05b editor's
 * TalkBack actions (no drag in a sheet).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExercisesSheet(
    title: String,
    exercises: List<ActiveExercise>,
    currentIndex: Int,
    reorder: Boolean,
    onSelect: (Int) -> Unit,
    onMove: (exerciseId: String, delta: Int) -> Unit,
    onAddExercise: () -> Unit,
    onReorder: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val d = GymBuddyTheme.dimens
    val done = exercises.count { it.allDone && it.sets.isNotEmpty() }
    AppBottomSheet(
        onDismiss = onDismiss,
        title = title,
        subtitle = joinDot(
            pluralStringResource(R.plurals.exercise_count, exercises.size, exercises.size),
            stringResource(R.string.workout_exercises_done, done),
        ),
    ) {
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(d.hairline),
        ) {
            exercises.forEachIndexed { i, ex ->
                ExerciseSheetRow(
                    exercise = ex,
                    current = i == currentIndex,
                    reorder = reorder,
                    canMoveUp = i > 0,
                    canMoveDown = i < exercises.lastIndex,
                    onClick = { onSelect(i) },
                    onMove = { delta -> onMove(ex.id, delta) },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = d.group),
            horizontalArrangement = Arrangement.spacedBy(d.stack),
        ) {
            OutlineButton(
                text = stringResource(R.string.workout_add_exercise_button),
                onClick = onAddExercise,
                modifier = Modifier.weight(1f),
            )
            if (reorder) {
                PrimaryButton(
                    text = stringResource(R.string.workout_reorder_done),
                    onClick = { onReorder(false) },
                    modifier = Modifier.weight(1f),
                )
            } else {
                OutlineButton(
                    text = stringResource(R.string.workout_reorder),
                    onClick = { onReorder(true) },
                    enabled = exercises.size > 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ExerciseSheetRow(
    exercise: ActiveExercise,
    current: Boolean,
    reorder: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMove: (Int) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val c = GymBuddyTheme.colors
    val d = GymBuddyTheme.dimens
    val allDone = exercise.allDone && exercise.sets.isNotEmpty()
    val moveUp = stringResource(R.string.a11y_move_up)
    val moveDown = stringResource(R.string.a11y_move_down)
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (current) cs.surfaceContainer else Color.Transparent,
        // Cobalt only for "you are here".
        border = if (current) BorderStroke(d.strokeOutline, c.cobalt) else null,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                selected = current
                customActions = buildList {
                    if (canMoveUp) add(CustomAccessibilityAction(moveUp) { onMove(-1); true })
                    if (canMoveDown) add(CustomAccessibilityAction(moveDown) { onMove(1); true })
                }
            },
    ) {
        Row(
            Modifier
                .clickable(enabled = !reorder, onClick = onClick)
                .heightIn(min = d.rowSet)
                .padding(start = d.stack, end = d.hairline),
            horizontalArrangement = Arrangement.spacedBy(d.group),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (allDone) {
                Surface(shape = FullShape, color = c.limeTint, modifier = Modifier.size(Mark)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(AppIcons.Ui.Check), null, Modifier.size(d.iconSmall), tint = cs.primary)
                    }
                }
            } else {
                Box(Modifier.size(Mark).border(d.strokeOutline, if (current) c.cobalt else cs.outline, FullShape))
            }
            Text(
                exercise.exerciseName,
                style = MaterialTheme.typography.titleSmall,
                color = if (allDone) cs.onSurfaceVariant else cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (reorder) {
                AppIconButton(AppIcons.Ui.ChevronUp, moveUp, { onMove(-1) }, enabled = canMoveUp)
                AppIconButton(AppIcons.Ui.ChevronDown, moveDown, { onMove(1) }, enabled = canMoveDown)
            } else {
                val doneSets = exercise.sets.count { it.isCompleted }
                Text(
                    stringResource(R.string.workout_sets_progress, doneSets, exercise.sets.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(end = d.stack),
                )
            }
        }
    }
}

/** ⋯ → Workout note: one text field, saved as the session's notes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteSheet(note: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val d = GymBuddyTheme.dimens
    var text by rememberSaveable { mutableStateOf(note) }
    AppBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.workout_note_title)) {
        AppTextField(
            value = text,
            onValueChange = { text = it },
            label = stringResource(R.string.workout_note_label),
            placeholder = stringResource(R.string.workout_note_placeholder),
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            text = stringResource(R.string.workout_note_save),
            onClick = { onSave(text) },
            modifier = Modifier.fillMaxWidth().padding(top = d.group),
        )
    }
}

/**
 * ⋯ → Rest timer: the shown exercise's rest, typed m:ss from the right
 * like the 05b editor ("130" = 1:30). Below 0:01 Save is off. The switch
 * on top is the opt-in rest buzz (06c; the Settings row comes in 7.10).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RestSheet(
    exerciseName: String,
    seconds: Int,
    buzz: Boolean,
    onBuzzChange: (Boolean) -> Unit,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val d = GymBuddyTheme.dimens
    var text by rememberSaveable { mutableStateOf(KeypadValue.clockDigits(seconds)) }
    val parsed = KeypadValue.parseClock(text)
    AppBottomSheet(onDismiss = onDismiss, title = null) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = d.group),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.workout_rest_buzz),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            AppSwitch(checked = buzz, onCheckedChange = onBuzzChange)
        }
        Keypad(
            label = stringResource(R.string.workout_rest_keypad_label, exerciseName),
            value = KeypadValue.clockText(text),
            unit = "",
            decimalEnabled = false,
            nextEnabled = (parsed ?: 0) > 0,
            nextLabel = stringResource(R.string.workout_keypad_save),
            onInput = { input ->
                when (input) {
                    KeypadInput.Cancel -> onDismiss()
                    KeypadInput.Next -> parsed?.takeIf { it > 0 }?.let(onSave)
                    else -> text = KeypadValue.apply(text, input, KeypadValue.Kind.Clock)
                }
            },
        )
    }
}
