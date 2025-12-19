package com.rukavina.gymbuddy.ui.exercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rukavina.gymbuddy.data.model.DifficultyLevel
import com.rukavina.gymbuddy.data.model.Equipment
import com.rukavina.gymbuddy.data.model.Exercise
import com.rukavina.gymbuddy.data.model.ExerciseCategory
import com.rukavina.gymbuddy.data.model.ExerciseType
import com.rukavina.gymbuddy.data.model.MuscleGroup

/**
 * Exercise management screen with CRUD operations.
 * Shows list of exercises and allows create, edit, delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreen(
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<Exercise?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercises") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingExercise = null
                    showDialog = true
                }
            ) {
                Icon(Icons.Default.Add, "Add Exercise")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.exercises.isEmpty() -> {
                    Text(
                        "No exercises yet. Tap + to add one!",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.exercises) { exercise ->
                            ExerciseItem(
                                exercise = exercise,
                                onEdit = {
                                    editingExercise = exercise
                                    showDialog = true
                                },
                                onDelete = {
                                    viewModel.deleteExercise(exercise.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Show success/error messages
        uiState.errorMessage?.let { error ->
            LaunchedEffect(error) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearError()
            }
            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(error)
            }
        }

        uiState.successMessage?.let { success ->
            LaunchedEffect(success) {
                kotlinx.coroutines.delay(2000)
                viewModel.clearSuccess()
            }
            Snackbar(
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text(success)
            }
        }

        // Create/Edit Dialog
        if (showDialog) {
            ExerciseFormDialog(
                exercise = editingExercise,
                onDismiss = { showDialog = false },
                onSave = { exercise ->
                    if (editingExercise != null) {
                        viewModel.updateExercise(exercise)
                    } else {
                        viewModel.createExercise(exercise)
                    }
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun ExerciseItem(
    exercise: Exercise,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDetails = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with name and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        // Default exercise badge
                        if (!exercise.isCustom) {
                            SuggestionChip(
                                onClick = { },
                                label = { Text("Default", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    if (exercise.isCustom) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Difficulty and Type badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Difficulty badge with color
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            exercise.difficulty.name,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when (exercise.difficulty) {
                            DifficultyLevel.BEGINNER -> MaterialTheme.colorScheme.primaryContainer
                            DifficultyLevel.INTERMEDIATE -> MaterialTheme.colorScheme.secondaryContainer
                            DifficultyLevel.ADVANCED -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                    )
                )

                // Exercise type
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            exercise.exerciseType.name,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )

                // Category
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            exercise.category.name,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Muscles
            Text(
                text = "Primary: ${exercise.primaryMuscles.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (exercise.secondaryMuscles.isNotEmpty()) {
                Text(
                    text = "Secondary: ${exercise.secondaryMuscles.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Equipment
            Text(
                text = "Equipment: ${exercise.equipmentNeeded.joinToString(", ") { it.name.replace("_", " ").lowercase().capitalize() }}",
                style = MaterialTheme.typography.bodySmall
            )

            // Description preview
            exercise.description?.let { desc ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = desc.take(100) + if (desc.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Detail dialog
    if (showDetails) {
        ExerciseDetailDialog(
            exercise = exercise,
            onDismiss = { showDetails = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseFormDialog(
    exercise: Exercise?,
    onDismiss: () -> Unit,
    onSave: (Exercise) -> Unit
) {
    var name by remember { mutableStateOf(exercise?.name ?: "") }
    var selectedPrimary by remember { mutableStateOf(exercise?.primaryMuscles ?: emptyList()) }
    var selectedSecondary by remember { mutableStateOf(exercise?.secondaryMuscles ?: emptyList()) }
    var showPrimaryMenu by remember { mutableStateOf(false) }
    var showSecondaryMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (exercise != null) "Edit Exercise" else "New Exercise") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Primary Muscles
                Column {
                    Text("Primary Muscles *", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MuscleGroup.entries.forEach { muscle ->
                            FilterChip(
                                selected = selectedPrimary.contains(muscle),
                                onClick = {
                                    selectedPrimary = if (selectedPrimary.contains(muscle)) {
                                        selectedPrimary - muscle
                                    } else {
                                        selectedPrimary + muscle
                                    }
                                },
                                label = { Text(muscle.name) }
                            )
                        }
                    }
                }

                // Secondary Muscles
                Column {
                    Text("Secondary Muscles", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MuscleGroup.entries.forEach { muscle ->
                            FilterChip(
                                selected = selectedSecondary.contains(muscle),
                                onClick = {
                                    selectedSecondary = if (selectedSecondary.contains(muscle)) {
                                        selectedSecondary - muscle
                                    } else {
                                        selectedSecondary + muscle
                                    }
                                },
                                label = { Text(muscle.name) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && selectedPrimary.isNotEmpty()) {
                        onSave(
                            Exercise(
                                id = exercise?.id ?: "",
                                name = name,
                                primaryMuscles = selectedPrimary,
                                secondaryMuscles = selectedSecondary,
                                description = exercise?.description,
                                instructions = exercise?.instructions ?: emptyList(),
                                difficulty = exercise?.difficulty ?: DifficultyLevel.BEGINNER,
                                equipmentNeeded = exercise?.equipmentNeeded ?: listOf(Equipment.BODYWEIGHT),
                                category = exercise?.category ?: ExerciseCategory.STRENGTH,
                                exerciseType = exercise?.exerciseType ?: ExerciseType.COMPOUND,
                                videoUrl = exercise?.videoUrl,
                                thumbnailUrl = exercise?.thumbnailUrl,
                                isCustom = exercise?.isCustom ?: true,
                                createdBy = exercise?.createdBy
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && selectedPrimary.isNotEmpty()
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

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    // Simple flow row implementation
    Column(modifier = modifier) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailDialog(
    exercise: Exercise,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    if (!exercise.isCustom) {
                        SuggestionChip(
                            onClick = { },
                            label = { Text("Default") }
                        )
                    }
                }

                HorizontalDivider()

                // Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AssistChip(
                        onClick = { },
                        label = { Text(exercise.difficulty.name) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = when (exercise.difficulty) {
                                DifficultyLevel.BEGINNER -> MaterialTheme.colorScheme.primaryContainer
                                DifficultyLevel.INTERMEDIATE -> MaterialTheme.colorScheme.secondaryContainer
                                DifficultyLevel.ADVANCED -> MaterialTheme.colorScheme.tertiaryContainer
                            }
                        )
                    )
                    AssistChip(
                        onClick = { },
                        label = { Text(exercise.exerciseType.name) }
                    )
                }

                // Category and Equipment
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = exercise.category.name.replace("_", " "),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Equipment Needed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = exercise.equipmentNeeded.joinToString(", ") {
                            it.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() }
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Muscle Groups
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Muscle Groups",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Primary: ${exercise.primaryMuscles.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (exercise.secondaryMuscles.isNotEmpty()) {
                        Text(
                            text = "Secondary: ${exercise.secondaryMuscles.joinToString(", ")}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Description
                exercise.description?.let { desc ->
                    HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Instructions
                if (exercise.instructions.isNotEmpty()) {
                    HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Instructions",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        exercise.instructions.forEachIndexed { index, instruction ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = instruction,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
