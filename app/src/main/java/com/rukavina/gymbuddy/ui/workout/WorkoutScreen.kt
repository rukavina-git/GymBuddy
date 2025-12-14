package com.rukavina.gymbuddy.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.rukavina.gymbuddy.data.model.Exercise
import com.rukavina.gymbuddy.data.model.PerformedExercise
import com.rukavina.gymbuddy.data.model.WorkoutSession
import com.rukavina.gymbuddy.ui.exercise.ExerciseViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Workout session management screen with CRUD operations.
 * Shows list of workout sessions and allows create, edit, delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutSessionViewModel = hiltViewModel(),
    exerciseViewModel: ExerciseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val exerciseUiState by exerciseViewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingWorkoutSession by remember { mutableStateOf<WorkoutSession?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Sessions") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingWorkoutSession = null
                    showDialog = true
                }
            ) {
                Icon(Icons.Default.Add, "Add Workout Session")
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
                uiState.workoutSessions.isEmpty() -> {
                    Text(
                        "No workout sessions yet. Tap + to add one!",
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
                        items(uiState.workoutSessions) { workoutSession ->
                            WorkoutSessionItem(
                                workoutSession = workoutSession,
                                availableExercises = exerciseUiState.exercises,
                                onEdit = {
                                    editingWorkoutSession = workoutSession
                                    showDialog = true
                                },
                                onDelete = {
                                    viewModel.deleteWorkoutSession(workoutSession.id)
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
            WorkoutSessionFormDialog(
                workoutSession = editingWorkoutSession,
                onDismiss = { showDialog = false },
                onSave = { workoutSession ->
                    if (editingWorkoutSession != null) {
                        viewModel.updateWorkoutSession(workoutSession)
                    } else {
                        viewModel.createWorkoutSession(workoutSession)
                    }
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun WorkoutSessionItem(
    workoutSession: WorkoutSession,
    availableExercises: List<Exercise>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val date = remember(workoutSession.date) { dateFormat.format(Date(workoutSession.date)) }

    // Create exercise ID to name mapping
    val exerciseMap = remember(availableExercises) {
        availableExercises.associateBy { it.id }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${workoutSession.durationMinutes} minutes • ${workoutSession.performedExercises.size} exercises",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (workoutSession.performedExercises.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    workoutSession.performedExercises.forEach { exercise ->
                        val exerciseName = exerciseMap[exercise.exerciseId]?.name ?: "Unknown Exercise"
                        Text(
                            text = "• $exerciseName: ${exercise.sets}×${exercise.reps} @ ${exercise.weight}kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionFormDialog(
    workoutSession: WorkoutSession?,
    onDismiss: () -> Unit,
    onSave: (WorkoutSession) -> Unit
) {
    var durationMinutes by remember { mutableStateOf(workoutSession?.durationMinutes?.toString() ?: "") }
    var exerciseSets by remember { mutableStateOf("3") }
    var exerciseReps by remember { mutableStateOf("10") }
    var exerciseWeight by remember { mutableStateOf("20") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (workoutSession != null) "Edit Workout Session" else "New Workout Session") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Quick workout session form (simplified for testing)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = durationMinutes,
                    onValueChange = { durationMinutes = it.filter { char -> char.isDigit() } },
                    label = { Text("Duration (minutes)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    "Sample Exercise",
                    style = MaterialTheme.typography.labelLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = exerciseSets,
                        onValueChange = { exerciseSets = it.filter { char -> char.isDigit() } },
                        label = { Text("Sets") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = exerciseReps,
                        onValueChange = { exerciseReps = it.filter { char -> char.isDigit() } },
                        label = { Text("Reps") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = exerciseWeight,
                        onValueChange = { exerciseWeight = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Weight") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val duration = durationMinutes.toIntOrNull() ?: 0
                    val sets = exerciseSets.toIntOrNull() ?: 0
                    val reps = exerciseReps.toIntOrNull() ?: 0
                    val weight = exerciseWeight.toFloatOrNull() ?: 0f

                    if (duration > 0 && sets > 0 && reps > 0) {
                        val performedExercise = PerformedExercise(
                            id = "",
                            exerciseId = "sample-exercise-id",
                            weight = weight,
                            reps = reps,
                            sets = sets
                        )

                        onSave(
                            WorkoutSession(
                                id = workoutSession?.id ?: "",
                                date = workoutSession?.date ?: System.currentTimeMillis(),
                                durationMinutes = duration,
                                performedExercises = listOf(performedExercise)
                            )
                        )
                    }
                },
                enabled = durationMinutes.toIntOrNull() != null
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
