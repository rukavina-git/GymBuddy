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
import com.rukavina.gymbuddy.utils.UnitConverter
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
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var workoutToDelete by remember { mutableStateOf<WorkoutSession?>(null) }

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
                                preferredUnits = uiState.preferredUnits,
                                onEdit = {
                                    editingWorkoutSession = workoutSession
                                    showDialog = true
                                },
                                onDelete = {
                                    workoutToDelete = workoutSession
                                    showDeleteConfirmDialog = true
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

        // Delete Confirmation Dialog
        if (showDeleteConfirmDialog && workoutToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Delete Workout Session") },
                text = {
                    Text("Are you sure you want to delete this workout session? This action cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            workoutToDelete?.let { viewModel.deleteWorkoutSession(it.id) }
                            showDeleteConfirmDialog = false
                            workoutToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            workoutToDelete = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Create/Edit Dialog
        if (showDialog) {
            WorkoutSessionFormDialog(
                workoutSession = editingWorkoutSession,
                availableExercises = exerciseUiState.exercises,
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
    preferredUnits: com.rukavina.gymbuddy.data.model.PreferredUnits,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val date = remember(workoutSession.date) { dateFormat.format(Date(workoutSession.date)) }
    val time = remember(workoutSession.date) { timeFormat.format(Date(workoutSession.date)) }

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
                    text = workoutSession.title ?: "Workout",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$date $time",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Format duration as HH:MM:SS
                val hours = workoutSession.durationSeconds / 3600
                val minutes = (workoutSession.durationSeconds % 3600) / 60
                val seconds = workoutSession.durationSeconds % 60
                val durationText = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                Text(
                    text = "$durationText • ${workoutSession.performedExercises.size} exercises",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (workoutSession.performedExercises.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    workoutSession.performedExercises.forEach { exercise ->
                        val exerciseName = exerciseMap[exercise.exerciseId]?.name ?: "Unknown Exercise"
                        val setCount = exercise.sets.size
                        val totalReps = exercise.sets.sumOf { it.reps }
                        val avgWeightInKg = if (exercise.sets.isNotEmpty()) {
                            exercise.sets.map { it.weight }.average().toFloat()
                        } else 0f
                        // Convert weight from metric (kg) to user's preferred display unit
                        val displayWeight = UnitConverter.weightToDisplayUnit(avgWeightInKg, preferredUnits)
                        val weightUnit = UnitConverter.getWeightUnitLabel(preferredUnits)
                        Text(
                            text = "• $exerciseName: $setCount sets, $totalReps reps @ $displayWeight$weightUnit",
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
    availableExercises: List<Exercise>,
    onDismiss: () -> Unit,
    onSave: (WorkoutSession) -> Unit
) {
    val calendar = remember {
        Calendar.getInstance().apply {
            timeInMillis = workoutSession?.date ?: System.currentTimeMillis()
        }
    }

    var title by remember { mutableStateOf(workoutSession?.title ?: "Workout") }
    var isEditingTitle by remember { mutableStateOf(false) }

    // Convert seconds to HH:MM:SS for editing
    val totalSeconds = workoutSession?.durationSeconds ?: 0
    var durationHours by remember { mutableStateOf(totalSeconds / 3600) }
    var durationMinutes by remember { mutableStateOf((totalSeconds % 3600) / 60) }
    var durationSeconds by remember { mutableStateOf(totalSeconds % 60) }
    var selectedDate by remember { mutableStateOf(calendar.timeInMillis) }
    var selectedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }
    var performedExercises by remember {
        mutableStateOf<List<PerformedExercise>>(workoutSession?.performedExercises ?: emptyList())
    }
    var showExercisePicker by remember { mutableStateOf(false) }
    var editingExerciseIndex by remember { mutableStateOf<Int?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Create exercise ID to name mapping
    val exerciseMap = remember(availableExercises) {
        availableExercises.associateBy { it.id }
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val displayDate = remember(selectedDate) { dateFormat.format(Date(selectedDate)) }
    val displayTime = remember(selectedHour, selectedMinute) {
        String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditingTitle) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Workout Title") }
                    )
                    IconButton(onClick = { isEditingTitle = false }) {
                        Icon(Icons.Default.Add, "Done") // Using Add as checkmark
                    }
                } else {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = { isEditingTitle = true }) {
                        Icon(Icons.Default.Edit, "Edit Title")
                    }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Date", style = MaterialTheme.typography.labelSmall)
                                Text(displayDate, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Time", style = MaterialTheme.typography.labelSmall)
                                Text(displayTime, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                item {
                    Text("Duration (HH:MM:SS)", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = if (durationHours == 0) "" else durationHours.toString(),
                            onValueChange = {
                                durationHours = if (it.isEmpty()) 0 else (it.toIntOrNull() ?: 0)
                            },
                            label = { Text("H") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Text(":", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = if (durationMinutes == 0 && durationHours == 0) "" else durationMinutes.toString(),
                            onValueChange = {
                                val mins = if (it.isEmpty()) 0 else (it.toIntOrNull() ?: 0)
                                if (mins < 60) durationMinutes = mins
                            },
                            label = { Text("M") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Text(":", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = if (durationSeconds == 0 && durationMinutes == 0 && durationHours == 0) "" else durationSeconds.toString(),
                            onValueChange = {
                                val secs = if (it.isEmpty()) 0 else (it.toIntOrNull() ?: 0)
                                if (secs < 60) durationSeconds = secs
                            },
                            label = { Text("S") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Exercises (${performedExercises.size})",
                            style = MaterialTheme.typography.titleSmall
                        )
                        IconButton(onClick = { showExercisePicker = true }) {
                            Icon(Icons.Default.Add, "Add Exercise")
                        }
                    }
                }

                performedExercises.forEachIndexed { index, exercise ->
                    item {
                        val exerciseName = exerciseMap[exercise.exerciseId]?.name ?: "Unknown"
                        val setCount = exercise.sets.size

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(exerciseName, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "$setCount sets",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    IconButton(onClick = {
                                        editingExerciseIndex = index
                                        showExercisePicker = true
                                    }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            "Edit",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = {
                                        performedExercises = performedExercises.filterIndexed { i, _ -> i != index }
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val totalSeconds = (durationHours * 3600) + (durationMinutes * 60) + durationSeconds
                    if (totalSeconds > 0 && title.isNotBlank()) {
                        // Combine date and time
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            set(Calendar.HOUR_OF_DAY, selectedHour)
                            set(Calendar.MINUTE, selectedMinute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        onSave(
                            WorkoutSession(
                                id = workoutSession?.id ?: java.util.UUID.randomUUID().toString(),
                                date = cal.timeInMillis,
                                durationSeconds = totalSeconds,
                                title = title,
                                performedExercises = performedExercises
                            )
                        )
                    }
                },
                enabled = title.isNotBlank() && ((durationHours * 3600) + (durationMinutes * 60) + durationSeconds) > 0
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
        ExerciseEditDialog(
            availableExercises = availableExercises,
            existingExercise = editingExerciseIndex?.let { performedExercises[it] },
            onDismiss = {
                showExercisePicker = false
                editingExerciseIndex = null
            },
            onSave = { performedExercise ->
                performedExercises = if (editingExerciseIndex != null) {
                    performedExercises.mapIndexed { index, ex ->
                        if (index == editingExerciseIndex) performedExercise else ex
                    }
                } else {
                    performedExercises + performedExercise
                }
                showExercisePicker = false
                editingExerciseIndex = null
            }
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseEditDialog(
    availableExercises: List<Exercise>,
    existingExercise: PerformedExercise?,
    onDismiss: () -> Unit,
    onSave: (PerformedExercise) -> Unit
) {
    var selectedExerciseId by remember { mutableStateOf(existingExercise?.exerciseId ?: "") }

    // For new exercises, create sets with placeholder values that will be shown as empty
    // For existing exercises, use their actual values
    data class UiWorkoutSet(
        val id: String,
        val weight: String,
        val reps: String,
        val orderIndex: Int
    )

    var workoutSets by remember {
        mutableStateOf<List<UiWorkoutSet>>(
            if (existingExercise != null) {
                existingExercise.sets.map { set ->
                    UiWorkoutSet(
                        id = set.id,
                        weight = if (set.weight == 0f) "" else set.weight.toString(),
                        reps = if (set.reps == 0) "" else set.reps.toString(),
                        orderIndex = set.orderIndex
                    )
                }
            } else {
                listOf(
                    UiWorkoutSet(
                        id = java.util.UUID.randomUUID().toString(),
                        weight = "",
                        reps = "",
                        orderIndex = 0
                    )
                )
            }
        )
    }
    var expanded by remember { mutableStateOf(false) }

    val selectedExercise = remember(selectedExerciseId, availableExercises) {
        availableExercises.find { it.id == selectedExerciseId }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingExercise != null) "Edit Exercise" else "Add Exercise") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedExercise?.name ?: "Select Exercise",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Exercise") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            availableExercises.forEach { exercise ->
                                DropdownMenuItem(
                                    text = { Text(exercise.name) },
                                    onClick = {
                                        selectedExerciseId = exercise.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sets (${workoutSets.size})", style = MaterialTheme.typography.titleSmall)
                        IconButton(onClick = {
                            workoutSets = workoutSets + UiWorkoutSet(
                                id = java.util.UUID.randomUUID().toString(),
                                weight = workoutSets.lastOrNull()?.weight ?: "",
                                reps = workoutSets.lastOrNull()?.reps ?: "",
                                orderIndex = workoutSets.size
                            )
                        }) {
                            Icon(Icons.Default.Add, "Add Set")
                        }
                    }
                }

                workoutSets.forEachIndexed { index, set ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${index + 1}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.width(30.dp)
                                )
                                OutlinedTextField(
                                    value = set.reps,
                                    onValueChange = { newReps ->
                                        workoutSets = workoutSets.mapIndexed { i, s ->
                                            if (i == index) s.copy(reps = newReps.filter { it.isDigit() }) else s
                                        }
                                    },
                                    label = { Text("Reps") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = set.weight,
                                    onValueChange = { newWeight ->
                                        workoutSets = workoutSets.mapIndexed { i, s ->
                                            if (i == index) s.copy(weight = newWeight.filter { it.isDigit() || it == '.' }) else s
                                        }
                                    },
                                    label = { Text("kg") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = {
                                        workoutSets = workoutSets.filterIndexed { i, _ -> i != index }
                                            .mapIndexed { i, s -> s.copy(orderIndex = i) }
                                    },
                                    enabled = workoutSets.size > 1
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        "Delete Set",
                                        tint = if (workoutSets.size > 1) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedExerciseId.isNotEmpty() && workoutSets.isNotEmpty()) {
                        // Convert UI sets to domain model, filtering out empty sets
                        val domainSets = workoutSets
                            .filter { it.reps.isNotBlank() || it.weight.isNotBlank() }
                            .mapIndexed { index, uiSet ->
                                com.rukavina.gymbuddy.data.model.WorkoutSet(
                                    id = uiSet.id,
                                    weight = uiSet.weight.toFloatOrNull() ?: 0f,
                                    reps = uiSet.reps.toIntOrNull() ?: 0,
                                    orderIndex = index
                                )
                            }

                        if (domainSets.isNotEmpty()) {
                            onSave(
                                PerformedExercise(
                                    id = existingExercise?.id ?: java.util.UUID.randomUUID().toString(),
                                    exerciseId = selectedExerciseId,
                                    sets = domainSets
                                )
                            )
                        }
                    }
                },
                enabled = selectedExerciseId.isNotEmpty() && workoutSets.any { it.reps.isNotBlank() || it.weight.isNotBlank() }
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
