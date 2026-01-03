package com.rukavina.gymbuddy.ui.exercise.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.data.model.DifficultyLevel
import com.rukavina.gymbuddy.data.model.ExerciseCategory
import com.rukavina.gymbuddy.data.model.ExerciseType
import com.rukavina.gymbuddy.ui.exercise.ExerciseFormState

/**
 * Details section for exercise form (difficulty, category, type, description).
 * Contains optional fields that provide additional context about the exercise.
 *
 * @param formState Form state holder containing all form data
 * @param modifier Modifier for the root component
 */
@Composable
fun ExerciseFormDetails(
    formState: ExerciseFormState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Description
        OutlinedTextField(
            value = formState.description,
            onValueChange = { formState.description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            supportingText = { Text("Optional - Brief description") }
        )

        // Difficulty Level
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Difficulty Level",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DifficultyLevel.entries.forEach { level ->
                    FilterChip(
                        selected = formState.difficulty == level,
                        onClick = { formState.difficulty = level },
                        label = { Text(level.name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Category
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExerciseCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = formState.category == cat,
                        onClick = { formState.category = cat },
                        label = { Text(cat.name.replace("_", " ")) }
                    )
                }
            }
        }

        // Exercise Type
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Exercise Type",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ExerciseType.entries.forEach { type ->
                    FilterChip(
                        selected = formState.exerciseType == type,
                        onClick = { formState.exerciseType = type },
                        label = { Text(type.name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
