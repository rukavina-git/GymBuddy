package com.rukavina.gymbuddy.ui.exercise.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.Constants
import com.rukavina.gymbuddy.data.model.Equipment
import com.rukavina.gymbuddy.data.model.MuscleGroup
import com.rukavina.gymbuddy.ui.exercise.ExerciseFormState

/**
 * Advanced section for exercise form (equipment, instructions, media).
 * Contains optional advanced fields for detailed exercise information.
 *
 * @param formState Form state holder containing all form data
 * @param modifier Modifier for the root component
 */
@Composable
fun ExerciseFormAdvanced(
    formState: ExerciseFormState,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Secondary Muscles
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Secondary Muscles",
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MuscleGroup.entries.forEach { muscle ->
                    FilterChip(
                        selected = formState.secondaryMuscles.contains(muscle),
                        onClick = {
                            formState.secondaryMuscles = if (formState.secondaryMuscles.contains(muscle)) {
                                formState.secondaryMuscles - muscle
                            } else {
                                formState.secondaryMuscles + muscle
                            }
                        },
                        label = { Text(muscle.name) }
                    )
                }
            }
        }

        // Equipment
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Equipment Needed",
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Equipment.entries.forEach { equip ->
                    FilterChip(
                        selected = formState.equipmentNeeded.contains(equip),
                        onClick = {
                            formState.equipmentNeeded = if (formState.equipmentNeeded.contains(equip)) {
                                formState.equipmentNeeded - equip
                            } else {
                                formState.equipmentNeeded + equip
                            }
                        },
                        label = { Text(equip.name.replace("_", " ")) }
                    )
                }
            }
        }

        // Instructions
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Step-by-Step Instructions",
                style = MaterialTheme.typography.labelLarge
            )

            formState.instructions.forEachIndexed { index, instruction ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    OutlinedTextField(
                        value = instruction,
                        onValueChange = {
                            if (it.length <= Constants.Exercise.MAX_INSTRUCTION_LENGTH) {
                                formState.instructions[index] = it
                            }
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter step ${index + 1}") },
                        minLines = 2,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        supportingText = {
                            Text("${instruction.length}/${Constants.Exercise.MAX_INSTRUCTION_LENGTH}")
                        }
                    )

                    IconButton(onClick = { formState.instructions.removeAt(index) }) {
                        Icon(Icons.Default.Delete, "Delete step")
                    }
                }
            }

            if (formState.instructions.size < Constants.Exercise.MAX_INSTRUCTIONS) {
                OutlinedButton(
                    onClick = { formState.instructions.add("") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Step (${formState.instructions.size}/${Constants.Exercise.MAX_INSTRUCTIONS})")
                }
            }
        }

        // Tips
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Tips & Advice",
                style = MaterialTheme.typography.labelLarge
            )

            formState.tips.forEachIndexed { index, tip ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = tip,
                        onValueChange = {
                            if (it.length <= Constants.Exercise.MAX_TIP_LENGTH) {
                                formState.tips[index] = it
                            }
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter tip") },
                        minLines = 2,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        supportingText = {
                            Text("${tip.length}/${Constants.Exercise.MAX_TIP_LENGTH}")
                        }
                    )

                    IconButton(onClick = { formState.tips.removeAt(index) }) {
                        Icon(Icons.Default.Delete, "Delete tip")
                    }
                }
            }

            if (formState.tips.size < Constants.Exercise.MAX_TIPS) {
                OutlinedButton(
                    onClick = { formState.tips.add("") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Tip (${formState.tips.size}/${Constants.Exercise.MAX_TIPS})")
                }
            }
        }

        // Media URLs
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Media",
                style = MaterialTheme.typography.labelLarge
            )

            OutlinedTextField(
                value = formState.videoUrl,
                onValueChange = { formState.videoUrl = it },
                label = { Text("Video URL") },
                placeholder = { Text("https://youtube.com/watch?v=...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("Optional") }
            )

            OutlinedTextField(
                value = "",
                onValueChange = { },
                label = { Text("Thumbnail Image") },
                placeholder = { Text("Gallery upload or default thumbnails") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = false,
                supportingText = { Text("To be implemented") }
            )
        }
    }
}
