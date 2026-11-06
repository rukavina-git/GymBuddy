package com.rukavina.gymbuddy.ui.exercises.page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.design.AppBottomSheet
import com.rukavina.gymbuddy.ui.design.AppChip
import com.rukavina.gymbuddy.ui.design.AppTextButton
import com.rukavina.gymbuddy.ui.design.AppTextField
import com.rukavina.gymbuddy.ui.design.DangerButton
import com.rukavina.gymbuddy.ui.design.Keypad
import com.rukavina.gymbuddy.ui.design.KeypadInput
import com.rukavina.gymbuddy.ui.design.KeypadValue
import com.rukavina.gymbuddy.ui.design.ListRow
import com.rukavina.gymbuddy.ui.design.PrimaryButton
import com.rukavina.gymbuddy.ui.template.clock
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

/** Rest choices on R13-10, in seconds; "Other" types any time. */
val RestChoices = listOf(60, 90, 120, 150, 180, 240, 300)

/**
 * Rest for this exercise (Figma R13-10): 1:00…5:00, Other (keypad m:ss
 * from the right, like the workout), and "Use my default" to clear it.
 * [seconds] null = no rest of its own yet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseRestSheet(
    exerciseName: String,
    seconds: Int?,
    defaultSeconds: Int,
    onPick: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val d = GymBuddyTheme.dimens
    var other by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf(KeypadValue.clockDigits(seconds ?: defaultSeconds)) }
    if (other) {
        val parsed = KeypadValue.parseClock(text)
        AppBottomSheet(onDismiss = onDismiss, title = null) {
            Keypad(
                label = stringResource(R.string.exercise_rest_keypad, exerciseName),
                value = KeypadValue.clockText(text),
                unit = "",
                decimalEnabled = false,
                nextEnabled = (parsed ?: 0) > 0,
                nextLabel = stringResource(R.string.exercise_rest_save),
                onInput = { input ->
                    when (input) {
                        KeypadInput.Cancel -> other = false
                        KeypadInput.Next -> parsed?.takeIf { it > 0 }?.let(onPick)
                        else -> text = KeypadValue.apply(text, input, KeypadValue.Kind.Clock)
                    }
                },
            )
        }
        return
    }
    AppBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.exercise_rest_title), subtitle = exerciseName) {
        val cells: List<Int?> = RestChoices + null
        cells.chunked(REST_COLUMNS).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.stackTight)) {
                row.forEach { value ->
                    val label = value?.let(::clock) ?: stringResource(R.string.exercise_rest_other)
                    val selected = if (value == null) seconds != null && seconds !in RestChoices else seconds == value
                    AppChip(
                        label = label,
                        selected = selected,
                        onClick = { if (value == null) other = true else onPick(value) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Text(
            stringResource(R.string.exercise_rest_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = d.hairline),
        )
        AppTextButton(
            text = stringResource(R.string.exercise_rest_default, clock(defaultSeconds)),
            onClick = { onPick(null) },
            modifier = Modifier.padding(top = d.stackTight),
        )
    }
}

private const val REST_COLUMNS = 4

/**
 * Your note for this exercise (Figma R13-14): shown everywhere, edited
 * here. With a note and [editing] false it reads first, "Edit note"
 * opens the field; otherwise the field is open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseNoteSheet(
    exerciseName: String,
    note: String?,
    editing: Boolean,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val d = GymBuddyTheme.dimens
    var edit by rememberSaveable { mutableStateOf(editing || note.isNullOrBlank()) }
    var text by rememberSaveable { mutableStateOf(note.orEmpty()) }
    AppBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(R.string.exercise_note_title),
        subtitle = stringResource(R.string.exercise_note_subtitle, exerciseName),
    ) {
        if (!edit) {
            Text(note.orEmpty(), style = MaterialTheme.typography.bodyLarge)
            AppTextButton(
                text = stringResource(R.string.exercise_edit_note),
                onClick = { edit = true },
                icon = AppIcons.Ui.Edit,
                modifier = Modifier.padding(top = d.group),
            )
        } else {
            AppTextField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(R.string.exercise_note_label),
                placeholder = stringResource(R.string.exercise_note_placeholder),
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                text = stringResource(R.string.exercise_note_save),
                onClick = { onSave(text) },
                modifier = Modifier.fillMaxWidth().padding(top = d.group),
            )
        }
    }
}

/** ⋯ → Add to template: your templates, tap one to append it there. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToTemplateSheet(templates: List<TemplateChoice>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AppBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.exercise_add_to_template_title)) {
        if (templates.isEmpty()) {
            Text(
                stringResource(R.string.exercise_add_to_template_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        templates.forEach { t -> ListRow(title = t.title, onClick = { onPick(t.id) }) }
    }
}

/**
 * Delete your own exercise (Figma R13-11): names the templates that use
 * it (they keep it), "Hide instead" above, Cancel left, red Delete right.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteExerciseDialog(
    name: String,
    usedIn: List<String>,
    onDelete: () -> Unit,
    onHide: () -> Unit,
    onDismiss: () -> Unit,
) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = cs.surfaceContainerHigh) {
            Column(
                modifier = Modifier.padding(start = d.insetDialog, end = d.insetDialog, top = d.insetDialog, bottom = d.group),
                verticalArrangement = Arrangement.spacedBy(d.group),
            ) {
                Text(stringResource(R.string.exercise_delete_title, name), style = MaterialTheme.typography.headlineSmall)
                if (usedIn.isNotEmpty()) {
                    Text(
                        stringResource(R.string.exercise_delete_used, listText(usedIn)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                    )
                }
                Text(stringResource(R.string.exercise_delete_past), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = d.target)
                        .clickable(role = Role.Button, onClick = onHide)
                        .padding(vertical = d.hairline),
                    verticalArrangement = Arrangement.spacedBy(d.hairline),
                ) {
                    Text(stringResource(R.string.exercise_delete_hide), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.exercise_delete_hide_body), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AppTextButton(stringResource(R.string.cancel), onDismiss)
                    Spacer(Modifier.weight(1f))
                    DangerButton(stringResource(R.string.exercise_delete_confirm), onDelete)
                }
            }
        }
    }
}

/** "Abs Day and Core", "A, B and C". */
@Composable
internal fun listText(items: List<String>): String = when (items.size) {
    0 -> ""
    1 -> items[0]
    else -> stringResource(R.string.list_and, items.dropLast(1).joinToString(", "), items.last())
}
