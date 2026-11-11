package com.rukavina.gymbuddy.ui.exercises.create

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.ui.design.FullScreenEditor
import com.rukavina.gymbuddy.ui.design.AppTextField
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

/**
 * Create C (Figma R13-6): from a search with no match, in the Exercises
 * tab and the picker. Name comes prefilled from the search text (words
 * capitalised); equipment, steps and rest are added later on its page.
 * F3b: a full screen, not a sheet, so the keyboard has room.
 */
@Composable
fun QuickCreateScreen(
    initialName: String,
    busy: Boolean,
    onCreate: (ExerciseForm) -> Unit,
    onDismiss: () -> Unit,
) {
    val d = GymBuddyTheme.dimens
    var name by rememberSaveable { mutableStateOf(titleCase(initialName)) }
    var tracking by rememberSaveable { mutableStateOf(ExerciseTrackingType.WEIGHT_REPS) }
    var muscle by rememberSaveable { mutableStateOf<MuscleGroup?>(null) }
    FullScreenEditor(
        title = stringResource(R.string.create_title_new),
        onClose = onDismiss,
        actionLabel = stringResource(R.string.create_create),
        onAction = { onCreate(ExerciseForm(name = name, tracking = tracking, mainMuscle = muscle)) },
        actionEnabled = name.isNotBlank() && !busy,
        actionLoading = busy,
    ) {
        AppTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.create_name),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        FormLabel(stringResource(R.string.create_what_you_log))
        TrackingChips(tracking) { tracking = it }
        FormLabel(stringResource(R.string.create_main_muscle))
        MuscleChips(muscle) { muscle = it }
        Text(
            stringResource(R.string.create_quick_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = d.stack),
        )
    }
}

/** "zercher squat" → "Zercher Squat" (R13-6); words already capitalised stay. */
fun titleCase(text: String): String =
    text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        .joinToString(" ") { w -> w.replaceFirstChar { it.titlecase() } }
