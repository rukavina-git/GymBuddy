package com.rukavina.gymbuddy.ui.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rukavina.gymbuddy.data.model.Exercise
import com.rukavina.gymbuddy.data.model.TemplateExercise
import com.rukavina.gymbuddy.data.model.WorkoutTemplate
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutViewModel
import java.util.UUID

/**
 * Main screen for viewing and managing workout templates.
 * Displays a list of templates with search functionality and
 * allows creating, editing, and deleting templates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTemplateScreen(
    viewModel: WorkoutTemplateViewModel = hiltViewModel(),
    activeWorkoutViewModel: ActiveWorkoutViewModel = hiltViewModel(),
    onStartWorkout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeWorkoutState by activeWorkoutViewModel.uiState.collectAsState()
    var showCreateEditDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var templateToDelete by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var templateToHide by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var viewingTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var templateToStart by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchTemplates(it)
                },
                label = { Text("Search templates") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            // Content area
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    uiState.templates.isEmpty() -> {
                        Text(
                            "No templates yet. Tap + to create one!",
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
                            items(uiState.templates) { template ->
                                WorkoutTemplateItem(
                                    template = template,
                                    availableExercises = uiState.availableExercises,
                                    onClick = {
                                        viewingTemplate = template
                                    },
                                    onEdit = {
                                        editingTemplate = template
                                        showCreateEditDialog = true
                                    },
                                    onDelete = {
                                        templateToDelete = template
                                    },
                                    onHide = {
                                        templateToHide = template
                                    },
                                    onStartWorkout = {
                                        // Check if there's an active workout
                                        if (activeWorkoutViewModel.hasActiveWorkout()) {
                                            templateToStart = template
                                        } else {
                                            activeWorkoutViewModel.startWorkoutFromTemplate(template)
                                            onStartWorkout()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Success/Error messages
                uiState.errorMessage?.let { error ->
                    LaunchedEffect(error) {
                        kotlinx.coroutines.delay(3000)
                        viewModel.clearError()
                    }
                    Snackbar(modifier = Modifier.padding(16.dp).align(Alignment.BottomCenter)) {
                        Text(error)
                    }
                }

                uiState.successMessage?.let { success ->
                    LaunchedEffect(success) {
                        kotlinx.coroutines.delay(2000)
                        viewModel.clearSuccess()
                    }
                    Snackbar(
                        modifier = Modifier.padding(16.dp).align(Alignment.BottomCenter),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(success)
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                editingTemplate = null
                showCreateEditDialog = true
            },
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "Create Template")
        }

        // Create/Edit Dialog
        if (showCreateEditDialog) {
            WorkoutTemplateFormDialog(
                template = editingTemplate,
                availableExercises = uiState.availableExercises,
                onDismiss = { showCreateEditDialog = false },
                onSave = { template ->
                    if (editingTemplate != null) {
                        viewModel.updateTemplate(template)
                    } else {
                        viewModel.createTemplate(template)
                    }
                    showCreateEditDialog = false
                }
            )
        }

        // Delete template confirmation dialog
        templateToDelete?.let { template ->
            AlertDialog(
                onDismissRequest = { templateToDelete = null },
                title = { Text("Delete Template?") },
                text = {
                    Text("Are you sure you want to delete \"${template.title}\"? This action cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteTemplate(template.id)
                            templateToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { templateToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Hide template confirmation dialog
        templateToHide?.let { template ->
            AlertDialog(
                onDismissRequest = { templateToHide = null },
                title = { Text("Hide Template?") },
                text = {
                    Text("\"${template.title}\" will be hidden from your template list. You can restore it from App Preferences.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.hideTemplate(template.id)
                            templateToHide = null
                        }
                    ) {
                        Text("Hide")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { templateToHide = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // View template dialog (read-only for default templates)
        viewingTemplate?.let { template ->
            WorkoutTemplateViewDialog(
                template = template,
                availableExercises = uiState.availableExercises,
                onDismiss = { viewingTemplate = null },
                onStartWorkout = {
                    viewingTemplate = null
                    // Check if there's an active workout
                    if (activeWorkoutViewModel.hasActiveWorkout()) {
                        templateToStart = template
                    } else {
                        activeWorkoutViewModel.startWorkoutFromTemplate(template)
                        onStartWorkout()
                    }
                }
            )
        }

        // Active workout confirmation dialog
        templateToStart?.let { template ->
            AlertDialog(
                onDismissRequest = { templateToStart = null },
                title = { Text("Active Workout in Progress") },
                text = {
                    Column {
                        Text("You have an active workout: \"${activeWorkoutState.workoutTitle}\"")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Starting a new workout will discard your current session. Do you want to continue?")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            activeWorkoutViewModel.discardWorkout()
                            activeWorkoutViewModel.startWorkoutFromTemplate(template)
                            templateToStart = null
                            onStartWorkout()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Discard & Start New")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                templateToStart = null
                                onStartWorkout() // Navigate to continue active workout
                            }
                        ) {
                            Text("Continue Active Workout")
                        }
                        TextButton(onClick = { templateToStart = null }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    }
}

/**
 * Card component for displaying a workout template in the list.
 * Shows template title, exercise count, exercise preview, and action buttons.
 */
@Composable
fun WorkoutTemplateItem(
    template: WorkoutTemplate,
    availableExercises: List<Exercise>,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onHide: () -> Unit,
    onStartWorkout: () -> Unit
) {
    // Create exercise ID to name mapping
    val exerciseMap = remember(availableExercises) {
        availableExercises.associateBy { it.id }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with title and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${template.templateExercises.size} exercises",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    if (template.isDefault) {
                        // Default templates can only be hidden, not edited or deleted
                        IconButton(onClick = onHide) {
                            Icon(Icons.Default.VisibilityOff, "Hide", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        // Custom templates can be edited and deleted
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Exercise preview list (sorted by orderIndex)
            if (template.templateExercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                template.templateExercises
                    .sortedBy { it.orderIndex }
                    .take(3) // Show first 3 exercises
                    .forEach { templateExercise ->
                        val exerciseName = exerciseMap[templateExercise.exerciseId]?.name ?: "Unknown"
                        Text(
                            text = "• $exerciseName: ${templateExercise.plannedSets}x${templateExercise.plannedReps}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                if (template.templateExercises.size > 3) {
                    Text(
                        text = "  +${template.templateExercises.size - 3} more...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            // Start Workout button
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStartWorkout,
                modifier = Modifier.fillMaxWidth(),
                enabled = template.templateExercises.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Icon(Icons.Default.PlayArrow, "Start", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Workout")
            }
        }
    }
}

/**
 * Dialog for creating or editing a workout template.
 * Manages template title, exercise list, and nested dialogs for exercise selection/configuration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTemplateFormDialog(
    template: WorkoutTemplate?,
    availableExercises: List<Exercise>,
    onDismiss: () -> Unit,
    onSave: (WorkoutTemplate) -> Unit
) {
    // Form state
    var title by remember { mutableStateOf(template?.title ?: "") }
    var templateExercises by remember {
        mutableStateOf(template?.templateExercises ?: emptyList())
    }
    var isTitleEditing by remember { mutableStateOf(template == null) } // Auto-edit for new templates

    // Dialog states
    var showExercisePicker by remember { mutableStateOf(false) }
    var editingExerciseIndex by remember { mutableStateOf<Int?>(null) }
    var showExerciseConfig by remember { mutableStateOf(false) }
    var pendingExerciseId by remember { mutableStateOf<Int?>(null) }
    var exerciseToDelete by remember { mutableStateOf<TemplateExercise?>(null) }

    // Create exercise map for lookups
    val exerciseMap = remember(availableExercises) {
        availableExercises.associateBy { it.id }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isTitleEditing) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Template name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge
                    )
                } else {
                    Text(
                        text = title.ifBlank { "Workout Template" },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { isTitleEditing = true }) {
                        Icon(Icons.Default.Edit, "Edit title", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Exercise list section
                Text(
                    "Exercises",
                    style = MaterialTheme.typography.labelLarge
                )

                if (templateExercises.isEmpty()) {
                    Text(
                        "No exercises added yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                } else {
                    // Scrollable exercise list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = templateExercises.sortedBy { it.orderIndex },
                            key = { it.id }
                        ) { exercise ->
                            TemplateExerciseListItem(
                                templateExercise = exercise,
                                exerciseName = exerciseMap[exercise.exerciseId]?.name ?: "Unknown",
                                position = templateExercises.indexOf(exercise),
                                totalCount = templateExercises.size,
                                onMoveUp = {
                                    templateExercises = reorderExercise(
                                        templateExercises,
                                        exercise.id,
                                        -1
                                    )
                                },
                                onMoveDown = {
                                    templateExercises = reorderExercise(
                                        templateExercises,
                                        exercise.id,
                                        1
                                    )
                                },
                                onEdit = {
                                    editingExerciseIndex = templateExercises.indexOf(exercise)
                                    showExerciseConfig = true
                                },
                                onDelete = {
                                    exerciseToDelete = exercise
                                }
                            )
                        }
                    }
                }

                // Add exercise button
                OutlinedButton(
                    onClick = { showExercisePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, "Add Exercise", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Exercise")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        WorkoutTemplate(
                            id = template?.id ?: UUID.randomUUID().toString(),
                            title = title,
                            templateExercises = templateExercises
                        )
                    )
                },
                enabled = title.isNotBlank() && templateExercises.isNotEmpty()
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

    // Exercise Picker Dialog
    if (showExercisePicker) {
        ExercisePickerDialog(
            availableExercises = availableExercises,
            onDismiss = { showExercisePicker = false },
            onExerciseSelected = { exerciseId ->
                pendingExerciseId = exerciseId
                showExercisePicker = false
                editingExerciseIndex = null
                showExerciseConfig = true
            }
        )
    }

    // Exercise Configuration Dialog
    if (showExerciseConfig) {
        val editingExercise = editingExerciseIndex?.let {
            templateExercises.getOrNull(it)
        }

        ExerciseConfigDialog(
            exerciseId = pendingExerciseId ?: editingExercise?.exerciseId ?: 0,
            exerciseName = exerciseMap[pendingExerciseId ?: editingExercise?.exerciseId]?.name ?: "",
            initialSets = editingExercise?.plannedSets ?: 3,
            initialReps = editingExercise?.plannedReps ?: 10,
            initialRest = editingExercise?.restSeconds,
            initialNotes = editingExercise?.notes,
            onDismiss = {
                showExerciseConfig = false
                pendingExerciseId = null
                editingExerciseIndex = null
            },
            onSave = { sets, reps, rest, notes ->
                if (editingExerciseIndex != null) {
                    // Update existing
                    templateExercises = templateExercises.mapIndexed { index, ex ->
                        if (index == editingExerciseIndex) {
                            ex.copy(
                                plannedSets = sets,
                                plannedReps = reps,
                                restSeconds = rest,
                                notes = notes
                            )
                        } else ex
                    }
                } else {
                    // Add new
                    val newExercise = TemplateExercise(
                        id = System.currentTimeMillis().toInt(),
                        exerciseId = pendingExerciseId!!,
                        plannedSets = sets,
                        plannedReps = reps,
                        orderIndex = templateExercises.size,
                        restSeconds = rest,
                        notes = notes
                    )
                    templateExercises = templateExercises + newExercise
                }
                showExerciseConfig = false
                pendingExerciseId = null
                editingExerciseIndex = null
            }
        )
    }

    // Delete exercise confirmation dialog
    exerciseToDelete?.let { exercise ->
        val exerciseName = exerciseMap[exercise.exerciseId]?.name ?: "this exercise"
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text("Remove Exercise?") },
            text = { Text("Are you sure you want to remove \"$exerciseName\" from this template?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        templateExercises = templateExercises
                            .filter { it.id != exercise.id }
                            .mapIndexed { index, ex ->
                                ex.copy(orderIndex = index)
                            }
                        exerciseToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Read-only dialog for viewing a workout template's details.
 * Shows the template exercises without editing capabilities.
 */
@Composable
fun WorkoutTemplateViewDialog(
    template: WorkoutTemplate,
    availableExercises: List<Exercise>,
    onDismiss: () -> Unit,
    onStartWorkout: () -> Unit
) {
    val exerciseMap = remember(availableExercises) {
        availableExercises.associateBy { it.id }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.headlineSmall
                )
                if (template.isDefault) {
                    Text(
                        text = "Default Template",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                Text(
                    text = "${template.templateExercises.size} exercises",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(template.templateExercises.sortedBy { it.orderIndex }) { templateExercise ->
                        val exerciseName = exerciseMap[templateExercise.exerciseId]?.name ?: "Unknown"
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = exerciseName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "${templateExercise.plannedSets} sets x ${templateExercise.plannedReps} reps",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                templateExercise.restSeconds?.let { rest ->
                                    Text(
                                        text = "Rest: ${rest}s",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                templateExercise.notes?.let { notes ->
                                    Text(
                                        text = notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onStartWorkout,
                enabled = template.templateExercises.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Start Workout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Helper function for reordering exercises in a template.
 * @param exercises The current list of template exercises
 * @param exerciseId The ID of the exercise to move
 * @param direction -1 for up, +1 for down
 * @return Updated list with reordered exercises and updated orderIndex values
 */
private fun reorderExercise(
    exercises: List<TemplateExercise>,
    exerciseId: Int,
    direction: Int
): List<TemplateExercise> {
    val sorted = exercises.sortedBy { it.orderIndex }
    val currentIndex = sorted.indexOfFirst { it.id == exerciseId }
    val newIndex = (currentIndex + direction).coerceIn(0, sorted.size - 1)

    if (currentIndex == newIndex) return exercises

    val mutable = sorted.toMutableList()
    val item = mutable.removeAt(currentIndex)
    mutable.add(newIndex, item)

    return mutable.mapIndexed { index, exercise ->
        exercise.copy(orderIndex = index)
    }
}
