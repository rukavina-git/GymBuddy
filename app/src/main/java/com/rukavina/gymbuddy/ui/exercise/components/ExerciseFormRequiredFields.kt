package com.rukavina.gymbuddy.ui.exercise.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.data.model.MuscleGroup
import com.rukavina.gymbuddy.ui.exercise.ExerciseFormState

/**
 * Required fields section for exercise creation form.
 * Displays name input and primary muscle selection.
 *
 * @param formState Form state holder containing all form data and validation logic
 * @param titleFocusRequester Focus requester for auto-focusing the title field
 * @param modifier Modifier for the root component
 */
@Composable
fun ExerciseFormRequiredFields(
    formState: ExerciseFormState,
    titleFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Required Information",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Exercise Name
        OutlinedTextField(
            value = formState.name,
            onValueChange = { formState.name = it },
            label = { Text("Exercise Name") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(titleFocusRequester)
                .onFocusChanged { focusState ->
                    formState.onNameFocusChanged(focusState.isFocused)
                },
            singleLine = true,
            supportingText = {
                Text(
                    text = when {
                        formState.shouldShowNameError() -> "Required"
                        !formState.nameTouched -> "e.g., Barbell Bench Press"
                        else -> ""
                    },
                    color = when {
                        formState.shouldShowNameError() -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            },
            isError = formState.shouldShowNameError(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )

        // Primary Muscles
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Primary Muscles",
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MuscleGroup.entries.forEach { muscle ->
                    FilterChip(
                        selected = formState.primaryMuscles.contains(muscle),
                        onClick = {
                            formState.primaryMusclesTouched = true
                            formState.primaryMuscles = if (formState.primaryMuscles.contains(muscle)) {
                                formState.primaryMuscles - muscle
                            } else {
                                formState.primaryMuscles + muscle
                            }
                        },
                        label = { Text(muscle.name) }
                    )
                }
            }

            // Helper text or error
            Text(
                text = when {
                    formState.shouldShowPrimaryMusclesError() -> "Select at least one muscle group"
                    !formState.primaryMusclesTouched -> "Choose which muscles this exercise targets"
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    formState.shouldShowPrimaryMusclesError() -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
