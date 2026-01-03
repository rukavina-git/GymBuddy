package com.rukavina.gymbuddy.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.data.model.Exercise
import com.rukavina.gymbuddy.ui.components.ExpandableSection
import com.rukavina.gymbuddy.ui.exercise.components.ExerciseFormAdvanced
import com.rukavina.gymbuddy.ui.exercise.components.ExerciseFormDetails
import com.rukavina.gymbuddy.ui.exercise.components.ExerciseFormRequiredFields
import kotlinx.coroutines.delay

/**
 * Modal bottom sheet for creating or editing exercises.
 * Coordinates the exercise form UI and delegates to specialized components.
 *
 * Architecture:
 * - Uses ExerciseFormState for business logic and state management
 * - Delegates UI sections to focused composables
 * - Handles navigation and lifecycle
 *
 * @param exercise Existing exercise to edit, or null to create new
 * @param onDismiss Callback when bottom sheet is dismissed
 * @param onSave Callback when exercise is saved, receives the created/updated exercise
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCreationBottomSheet(
    exercise: Exercise?,
    onDismiss: () -> Unit,
    onSave: (Exercise) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val formState = remember(exercise) { ExerciseFormState(exercise) }
    val titleFocusRequester = remember { FocusRequester() }

    // Auto-focus title field on create (not edit)
    LaunchedEffect(Unit) {
        if (exercise == null) {
            delay(200) // Small delay for smooth animation
            titleFocusRequester.requestFocus()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Header
            Text(
                text = if (exercise != null) "Edit Exercise" else "New Exercise",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Create a custom exercise for your workouts",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Scrollable Form
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Required Fields
                ExerciseFormRequiredFields(
                    formState = formState,
                    titleFocusRequester = titleFocusRequester
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Details Section (Expandable)
                ExpandableSection(
                    title = "Details",
                    subtitle = "Description, difficulty, and category",
                    expanded = formState.detailsExpanded,
                    onExpandChange = { formState.detailsExpanded = it }
                ) {
                    ExerciseFormDetails(formState = formState)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Advanced Section (Expandable)
                ExpandableSection(
                    title = "Advanced",
                    subtitle = "Equipment, instructions, tips, and media",
                    expanded = formState.advancedExpanded,
                    onExpandChange = { formState.advancedExpanded = it }
                ) {
                    ExerciseFormAdvanced(formState = formState)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = { onSave(formState.toExercise()) },
                    enabled = formState.isValid(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Exercise")
                }
            }
        }
    }
}
