package com.rukavina.gymbuddy.ui.exercise

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseWizardDialog(
    exercise: Exercise?,
    onDismiss: () -> Unit,
    onSave: (Exercise) -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    val totalSteps = 5

    // Form state
    var name by remember { mutableStateOf(exercise?.name ?: "") }
    var description by remember { mutableStateOf(exercise?.description ?: "") }
    var difficulty by remember { mutableStateOf(exercise?.difficulty ?: DifficultyLevel.BEGINNER) }
    var category by remember { mutableStateOf(exercise?.category ?: ExerciseCategory.STRENGTH) }
    var exerciseType by remember { mutableStateOf(exercise?.exerciseType ?: ExerciseType.COMPOUND) }
    var primaryMuscles by remember { mutableStateOf(exercise?.primaryMuscles ?: emptyList()) }
    var secondaryMuscles by remember { mutableStateOf(exercise?.secondaryMuscles ?: emptyList()) }
    var equipmentNeeded by remember { mutableStateOf(exercise?.equipmentNeeded ?: listOf(Equipment.BODYWEIGHT)) }
    val instructions = remember { mutableStateListOf<String>().apply { addAll(exercise?.instructions ?: emptyList()) } }
    var videoUrl by remember { mutableStateOf(exercise?.videoUrl ?: "") }
    var thumbnailUrl by remember { mutableStateOf(exercise?.thumbnailUrl ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header with close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (exercise != null) "Edit Exercise" else "New Exercise",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                // Stepper
                LinearProgressIndicator(
                    progress = { (currentStep + 1) / totalSteps.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )

                // Step indicator
                Text(
                    text = "Step ${currentStep + 1} of $totalSteps",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                // Step content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {
                    when (currentStep) {
                        0 -> BasicInfoStep(
                            name = name,
                            onNameChange = { name = it },
                            description = description,
                            onDescriptionChange = { description = it }
                        )
                        1 -> ClassificationStep(
                            difficulty = difficulty,
                            onDifficultyChange = { difficulty = it },
                            category = category,
                            onCategoryChange = { category = it },
                            exerciseType = exerciseType,
                            onExerciseTypeChange = { exerciseType = it }
                        )
                        2 -> MusclesEquipmentStep(
                            primaryMuscles = primaryMuscles,
                            onPrimaryMusclesChange = { primaryMuscles = it },
                            secondaryMuscles = secondaryMuscles,
                            onSecondaryMusclesChange = { secondaryMuscles = it },
                            equipmentNeeded = equipmentNeeded,
                            onEquipmentChange = { equipmentNeeded = it }
                        )
                        3 -> InstructionsStep(
                            instructions = instructions
                        )
                        4 -> MediaStep(
                            videoUrl = videoUrl,
                            onVideoUrlChange = { videoUrl = it },
                            thumbnailUrl = thumbnailUrl,
                            onThumbnailUrlChange = { thumbnailUrl = it }
                        )
                    }
                }

                // Navigation buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back button
                    if (currentStep > 0) {
                        TextButton(
                            onClick = { currentStep-- }
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Next/Save button
                    if (currentStep < totalSteps - 1) {
                        FilledTonalButton(
                            onClick = { currentStep++ },
                            enabled = isStepValid(currentStep, name, primaryMuscles)
                        ) {
                            Text("Next")
                        }
                    } else {
                        Button(
                            onClick = {
                                onSave(
                                    Exercise(
                                        id = exercise?.id ?: 0,
                                        name = name,
                                        primaryMuscles = primaryMuscles,
                                        secondaryMuscles = secondaryMuscles,
                                        description = description.ifBlank { null },
                                        instructions = instructions.filter { it.isNotBlank() },
                                        difficulty = difficulty,
                                        equipmentNeeded = equipmentNeeded,
                                        category = category,
                                        exerciseType = exerciseType,
                                        videoUrl = videoUrl.ifBlank { null },
                                        thumbnailUrl = thumbnailUrl.ifBlank { null },
                                        isCustom = exercise?.isCustom ?: true,
                                        createdBy = exercise?.createdBy,
                                        isHidden = exercise?.isHidden ?: false
                                    )
                                )
                            },
                            enabled = name.isNotBlank() && primaryMuscles.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

private fun isStepValid(step: Int, name: String, primaryMuscles: List<MuscleGroup>): Boolean {
    return when (step) {
        0 -> name.isNotBlank()
        2 -> primaryMuscles.isNotEmpty()
        else -> true
    }
}

@Composable
fun BasicInfoStep(
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Basic Information",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Exercise Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text("Required") }
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            supportingText = { Text("Optional - Brief description of the exercise") }
        )
    }
}

@Composable
fun ClassificationStep(
    difficulty: DifficultyLevel,
    onDifficultyChange: (DifficultyLevel) -> Unit,
    category: ExerciseCategory,
    onCategoryChange: (ExerciseCategory) -> Unit,
    exerciseType: ExerciseType,
    onExerciseTypeChange: (ExerciseType) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Classification",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Difficulty
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Difficulty Level *",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DifficultyLevel.entries.forEach { level ->
                    FilterChip(
                        selected = difficulty == level,
                        onClick = { onDifficultyChange(level) },
                        label = { Text(level.name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Category
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Category *",
                style = MaterialTheme.typography.labelLarge
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExerciseCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { onCategoryChange(cat) },
                        label = { Text(cat.name.replace("_", " ")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Exercise Type
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Exercise Type *",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ExerciseType.entries.forEach { type ->
                    FilterChip(
                        selected = exerciseType == type,
                        onClick = { onExerciseTypeChange(type) },
                        label = { Text(type.name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun MusclesEquipmentStep(
    primaryMuscles: List<MuscleGroup>,
    onPrimaryMusclesChange: (List<MuscleGroup>) -> Unit,
    secondaryMuscles: List<MuscleGroup>,
    onSecondaryMusclesChange: (List<MuscleGroup>) -> Unit,
    equipmentNeeded: List<Equipment>,
    onEquipmentChange: (List<Equipment>) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Muscles & Equipment",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Primary Muscles
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Primary Muscles *",
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MuscleGroup.entries.forEach { muscle ->
                    FilterChip(
                        selected = primaryMuscles.contains(muscle),
                        onClick = {
                            onPrimaryMusclesChange(
                                if (primaryMuscles.contains(muscle)) {
                                    primaryMuscles - muscle
                                } else {
                                    primaryMuscles + muscle
                                }
                            )
                        },
                        label = { Text(muscle.name) }
                    )
                }
            }
        }

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
                        selected = secondaryMuscles.contains(muscle),
                        onClick = {
                            onSecondaryMusclesChange(
                                if (secondaryMuscles.contains(muscle)) {
                                    secondaryMuscles - muscle
                                } else {
                                    secondaryMuscles + muscle
                                }
                            )
                        },
                        label = { Text(muscle.name) }
                    )
                }
            }
        }

        // Equipment
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Equipment Needed *",
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Equipment.entries.forEach { equip ->
                    FilterChip(
                        selected = equipmentNeeded.contains(equip),
                        onClick = {
                            onEquipmentChange(
                                if (equipmentNeeded.contains(equip)) {
                                    equipmentNeeded - equip
                                } else {
                                    equipmentNeeded + equip
                                }
                            )
                        },
                        label = { Text(equip.name.replace("_", " ")) }
                    )
                }
            }
        }
    }
}

@Composable
fun InstructionsStep(
    instructions: SnapshotStateList<String>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Step-by-Step Instructions",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Add detailed instructions on how to perform this exercise",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Instructions list
        instructions.forEachIndexed { index, instruction ->
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
                    onValueChange = { newValue ->
                        instructions[index] = newValue
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter step ${index + 1}") },
                    minLines = 2,
                    maxLines = 4
                )

                IconButton(
                    onClick = {
                        instructions.removeAt(index)
                    }
                ) {
                    Icon(Icons.Default.Delete, "Delete step")
                }
            }
        }

        // Add step button
        OutlinedButton(
            onClick = {
                instructions.add("")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Step")
        }
    }
}

@Composable
fun MediaStep(
    videoUrl: String,
    onVideoUrlChange: (String) -> Unit,
    thumbnailUrl: String,
    onThumbnailUrlChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Media (Optional)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Add video and image URLs to help users learn the exercise",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = videoUrl,
            onValueChange = onVideoUrlChange,
            label = { Text("Video URL") },
            placeholder = { Text("https://youtube.com/watch?v=...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text("YouTube or other video platform URL") }
        )

        OutlinedTextField(
            value = thumbnailUrl,
            onValueChange = onThumbnailUrlChange,
            label = { Text("Thumbnail URL") },
            placeholder = { Text("https://example.com/image.jpg") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text("Image URL for preview") }
        )
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
