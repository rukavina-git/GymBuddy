package com.rukavina.gymbuddy.ui.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Dialog for configuring exercise details (sets, reps, rest time, notes).
 * Used when adding or editing an exercise in a workout template.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseConfigDialog(
    exerciseId: Int,
    exerciseName: String,
    initialSets: Int,
    initialReps: Int,
    initialRest: Int?,
    initialNotes: String?,
    onDismiss: () -> Unit,
    onSave: (sets: Int, reps: Int, rest: Int?, notes: String?) -> Unit
) {
    var sets by remember { mutableStateOf(initialSets.toString()) }
    var reps by remember { mutableStateOf(initialReps.toString()) }
    var rest by remember { mutableStateOf(initialRest?.toString() ?: "") }
    var notes by remember { mutableStateOf(initialNotes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Exercise") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Exercise name (read-only display)
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider()

                // Sets and Reps row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sets,
                        onValueChange = { sets = it.filter { char -> char.isDigit() } },
                        label = { Text("Sets *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it.filter { char -> char.isDigit() } },
                        label = { Text("Reps *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // Rest time (optional)
                OutlinedTextField(
                    value = rest,
                    onValueChange = { rest = it.filter { char -> char.isDigit() } },
                    label = { Text("Rest (seconds)") },
                    placeholder = { Text("Optional") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Notes (optional)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    placeholder = { Text("e.g., Focus on form") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val setsInt = sets.toIntOrNull()
                    val repsInt = reps.toIntOrNull()
                    val restInt = rest.toIntOrNull()

                    if (setsInt != null && repsInt != null && setsInt > 0 && repsInt > 0) {
                        onSave(
                            setsInt,
                            repsInt,
                            restInt,
                            notes.ifBlank { null }
                        )
                    }
                },
                enabled = sets.toIntOrNull() != null &&
                    reps.toIntOrNull() != null &&
                    sets.toIntOrNull()!! > 0 &&
                    reps.toIntOrNull()!! > 0
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
