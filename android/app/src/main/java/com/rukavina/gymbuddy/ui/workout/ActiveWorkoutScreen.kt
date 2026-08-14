package com.rukavina.gymbuddy.ui.workout

import androidx.compose.foundation.background
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.utils.validation.InputValidation
import com.rukavina.gymbuddy.utils.UnitConverter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main screen for an active workout session.
 * Shows timer, workout details, and exercise tracking.
 */
@Composable
fun ActiveWorkoutScreen(
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
    onWorkoutComplete: () -> Unit = {},
    onWorkoutDiscarded: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showStopDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Navigate to Workouts when workout is saved
    LaunchedEffect(uiState.workoutSaved) {
        if (uiState.workoutSaved) {
            onWorkoutComplete()
        }
    }

    // Navigate to Templates when workout is discarded
    LaunchedEffect(uiState.workoutDiscarded) {
        if (uiState.workoutDiscarded) {
            onWorkoutDiscarded()
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Fixed Header
            WorkoutHeader(
                workoutTitle = uiState.workoutTitle,
                startTime = uiState.workoutStartTime,
                elapsedSeconds = uiState.elapsedSeconds,
                isTimerRunning = uiState.isTimerRunning,
                onToggleTimer = { viewModel.toggleTimer() },
                onStop = { showStopDialog = true },
                onDiscard = { showDiscardDialog = true }
            )

            HorizontalDivider()

            // Exercise List
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    itemsIndexed(
                        items = uiState.exercises,
                        key = { _, exercise -> exercise.id }
                    ) { index, exercise ->
                        ExerciseCard(
                            exerciseNumber = index + 1,
                            exercise = exercise,
                            preferredUnits = uiState.preferredUnits,
                            onUpdateReps = { setId, reps ->
                                viewModel.updateSetReps(exercise.id, setId, reps)
                            },
                            onUpdateWeight = { setId, weight ->
                                viewModel.updateSetWeight(exercise.id, setId, weight)
                            },
                            onUpdateDuration = { setId, duration ->
                                viewModel.updateSetDuration(exercise.id, setId, duration)
                            },
                            onUpdateDistance = { setId, distance ->
                                viewModel.updateSetDistance(exercise.id, setId, distance)
                            },
                            onNoteClick = { setId ->
                                // TODO: Implement notes dialog
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        // Stop Confirmation Dialog
        if (showStopDialog) {
            AlertDialog(
                onDismissRequest = { showStopDialog = false },
                title = { Text("Are you done with the workout?") },
                text = { Text("This will save your workout and end the session.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showStopDialog = false
                            viewModel.saveWorkout()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Yes, I'm Done")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStopDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Discard Confirmation Dialog
        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text("Discard Workout?") },
                text = { Text("Are you sure you want to discard this workout? All progress will be lost.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDiscardDialog = false
                            viewModel.discardWorkout()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Discard")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Error message
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
    }
}

/**
 * Fixed header showing workout title, start time, timer, and control buttons.
 */
@Composable
fun WorkoutHeader(
    workoutTitle: String,
    startTime: Long,
    elapsedSeconds: Long,
    isTimerRunning: Boolean,
    onToggleTimer: () -> Unit,
    onStop: () -> Unit,
    onDiscard: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()) }
    val startTimeFormatted = remember(startTime) {
        if (startTime > 0) dateFormat.format(Date(startTime)) else ""
    }

    val timerText = remember(elapsedSeconds) {
        val hours = elapsedSeconds / 3600
        val minutes = (elapsedSeconds % 3600) / 60
        val seconds = elapsedSeconds % 60
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp)
        ) {
            // Title row with discard button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Workout Title
                Text(
                    text = workoutTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )

                // Discard button
                TextButton(onClick = onDiscard) {
                    Text(
                        "Discard",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Start time
            Text(
                text = startTimeFormatted,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Timer and controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timer (smaller)
                Text(
                    text = timerText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Control buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Pause/Play button
                    IconButton(
                        onClick = onToggleTimer,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small
                            )
                    ) {
                        Icon(
                            imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isTimerRunning) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Stop button
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.error,
                                shape = MaterialTheme.shapes.small
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card displaying a single exercise with all its sets.
 */
@Composable
fun ExerciseCard(
    exerciseNumber: Int,
    exercise: ActiveExercise,
    preferredUnits: PreferredUnits,
    onUpdateReps: (setId: String, reps: String) -> Unit,
    onUpdateWeight: (setId: String, weight: String) -> Unit,
    onUpdateDuration: (setId: String, duration: String) -> Unit,
    onUpdateDistance: (setId: String, distance: String) -> Unit,
    onNoteClick: (setId: String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Exercise header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Exercise number
                    Text(
                        text = "$exerciseNumber.",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Column {
                        Text(
                            text = exercise.exerciseName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${exercise.plannedSets} sets" +
                                (exercise.plannedReps?.let { " × $it reps" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Prescriptive guidance only - there is no rest timer in v1.
                        exercise.restSeconds?.let { rest ->
                            Text(
                                text = "Rest ${rest}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Expand/Collapse button
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Edit button
                    IconButton(onClick = { /* TODO: Edit exercise */ }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Sets list (only shown when expanded)
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                exercise.sets.forEach { set ->
                    SetRow(
                        set = set,
                        trackingType = exercise.exerciseTrackingType,
                        preferredUnits = preferredUnits,
                        onUpdateReps = { reps -> onUpdateReps(set.id, reps) },
                        onUpdateWeight = { weight -> onUpdateWeight(set.id, weight) },
                        onUpdateDuration = { duration -> onUpdateDuration(set.id, duration) },
                        onUpdateDistance = { distance -> onUpdateDistance(set.id, distance) },
                        onNoteClick = { onNoteClick(set.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Single set row with input fields for the measurements its exercise's
 * tracking type requires (weight, reps, duration, distance).
 */
@Composable
fun SetRow(
    set: WorkoutSet,
    trackingType: ExerciseTrackingType,
    preferredUnits: PreferredUnits,
    onUpdateReps: (String) -> Unit,
    onUpdateWeight: (String) -> Unit,
    onUpdateDuration: (String) -> Unit,
    onUpdateDistance: (String) -> Unit,
    onNoteClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set number
        Text(
            text = "Set ${set.setNumber}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(60.dp)
        )

        if (SetTrackingFields.showsReps(trackingType)) {
            OutlinedTextField(
                value = set.reps,
                onValueChange = { input ->
                    onUpdateReps(InputValidation.validateReps(input))
                },
                label = { Text("Reps") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        if (SetTrackingFields.showsWeight(trackingType)) {
            val weightUnit = UnitConverter.getWeightUnitLabel(preferredUnits)
            OutlinedTextField(
                value = set.weight,
                onValueChange = { input ->
                    InputValidation.validateWeight(input)?.let { onUpdateWeight(it) }
                },
                label = { Text("Weight ($weightUnit)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }

        if (SetTrackingFields.showsDuration(trackingType)) {
            OutlinedTextField(
                value = set.duration,
                onValueChange = { input ->
                    onUpdateDuration(InputValidation.validateDuration(input))
                },
                label = { Text("Duration (s)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        if (SetTrackingFields.showsDistance(trackingType)) {
            OutlinedTextField(
                value = set.distance,
                onValueChange = { input ->
                    InputValidation.validateDistance(input)?.let { onUpdateDistance(it) }
                },
                label = { Text("Distance (m)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }

        // Note icon
        IconButton(
            onClick = onNoteClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.NoteAlt,
                contentDescription = "Add Note",
                tint = if (set.notes.isNotBlank()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
