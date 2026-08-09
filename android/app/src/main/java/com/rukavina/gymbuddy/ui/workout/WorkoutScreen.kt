package com.rukavina.gymbuddy.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PerformedExercise
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutSet
import com.rukavina.gymbuddy.ui.exercise.ExerciseViewModel
import com.rukavina.gymbuddy.utils.validation.InputValidation
import com.rukavina.gymbuddy.utils.validation.ValidationConstants
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
    var showSortMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Sort control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sort by: ${getSortOrderLabel(uiState.sortOrder)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Default.Sort, "Sort")
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    WorkoutSessionSortOrder.entries.forEach { sortOrder ->
                        DropdownMenuItem(
                            text = { Text(getSortOrderLabel(sortOrder)) },
                            onClick = {
                                viewModel.setSortOrder(sortOrder)
                                showSortMenu = false
                            },
                            leadingIcon = if (uiState.sortOrder == sortOrder) {
                                { Icon(Icons.Default.Add, null) } // Using Add as checkmark
                            } else null
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
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
                                onEdit = {
                                    editingWorkoutSession = workoutSession
                                    showDialog = true
                                }
                            )
                        }
                    }
                }
            }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                editingWorkoutSession = null
                showDialog = true
            },
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "Add Workout Session")
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
                },
                onDelete = if (editingWorkoutSession != null) {
                    {
                        viewModel.deleteWorkoutSession(editingWorkoutSession!!.id)
                        showDialog = false
                        editingWorkoutSession = null
                    }
                } else null,
                generatePerformedExerciseId = viewModel::newPerformedExerciseId,
                generateSetId = viewModel::newWorkoutSetId,
                generateWorkoutSessionId = viewModel::newWorkoutSessionId
            )
        }
    }
}

@Composable
fun WorkoutSessionItem(
    workoutSession: WorkoutSession,
    onEdit: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val date = remember(workoutSession.startedAt) { dateFormat.format(Date(workoutSession.startedAt)) }
    val time = remember(workoutSession.startedAt) { timeFormat.format(Date(workoutSession.startedAt)) }

    // Format duration as HH:MM:SS
    val hours = workoutSession.durationSeconds / 3600
    val minutes = (workoutSession.durationSeconds % 3600) / 60
    val seconds = workoutSession.durationSeconds % 60
    val durationText = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
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
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = ValidationConstants.MAX_TITLE_LINES,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$date $time",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${workoutSession.performedExercises.size} exercises",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    onSave: (WorkoutSession) -> Unit,
    onDelete: (() -> Unit)? = null,
    generatePerformedExerciseId: () -> String,
    generateSetId: () -> String,
    generateWorkoutSessionId: () -> String
) {
    val calendar = remember {
        Calendar.getInstance().apply {
            timeInMillis = workoutSession?.startedAt ?: System.currentTimeMillis()
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
    var showDeleteWorkoutConfirm by remember { mutableStateOf(false) }
    var exerciseToDeleteIndex by remember { mutableStateOf<Int?>(null) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val displayDate = remember(selectedDate) { dateFormat.format(Date(selectedDate)) }
    val displayTime = remember(selectedHour, selectedMinute) {
        String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            // Title row with edit button on the left
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditingTitle) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = InputValidation.validateTitle(it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Workout Title") }
                    )
                    IconButton(onClick = { isEditingTitle = false }) {
                        Icon(Icons.Default.Check, "Done")
                    }
                } else {
                    IconButton(onClick = { isEditingTitle = true }) {
                        Icon(Icons.Default.Edit, "Edit Title")
                    }
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = ValidationConstants.MAX_TITLE_LINES,
                        overflow = TextOverflow.Ellipsis
                    )
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = if (durationHours == 0) "" else durationHours.toString(),
                            onValueChange = { input ->
                                val hours = input.filter { it.isDigit() }.toIntOrNull() ?: 0
                                if (hours <= 23) durationHours = hours
                            },
                            placeholder = { Text("00") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Text(":", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = if (durationMinutes == 0 && durationHours == 0) "" else durationMinutes.toString(),
                            onValueChange = { input ->
                                val mins = input.filter { it.isDigit() }.toIntOrNull() ?: 0
                                if (mins <= 59) durationMinutes = mins
                            },
                            placeholder = { Text("00") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Text(":", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = if (durationSeconds == 0 && durationMinutes == 0 && durationHours == 0) "" else durationSeconds.toString(),
                            onValueChange = { input ->
                                val secs = input.filter { it.isDigit() }.toIntOrNull() ?: 0
                                if (secs <= 59) durationSeconds = secs
                            },
                            placeholder = { Text("00") },
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
                        val exerciseName = exercise.exerciseName
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
                                        exerciseToDeleteIndex = index
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
                                id = workoutSession?.id ?: generateWorkoutSessionId(),
                                startedAt = cal.timeInMillis,
                                endedAt = workoutSession?.endedAt,
                                durationSeconds = totalSeconds,
                                title = title,
                                notes = workoutSession?.notes,
                                templateId = workoutSession?.templateId,
                                templateTitle = workoutSession?.templateTitle,
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Delete button (only for existing workouts)
                if (workoutSession != null && onDelete != null) {
                    TextButton(onClick = { showDeleteWorkoutConfirm = true }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
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
                }.mapIndexed { index, ex -> ex.copy(orderIndex = index) }
                showExercisePicker = false
                editingExerciseIndex = null
            },
            generatePerformedExerciseId = generatePerformedExerciseId,
            generateSetId = generateSetId
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

    // Delete workout confirmation dialog
    if (showDeleteWorkoutConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteWorkoutConfirm = false },
            title = { Text("Delete Workout?") },
            text = {
                Text("Are you sure you want to delete this workout? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteWorkoutConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteWorkoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Exercise delete confirmation dialog
    exerciseToDeleteIndex?.let { index ->
        val exerciseName = performedExercises.getOrNull(index)?.exerciseName ?: "this exercise"
        AlertDialog(
            onDismissRequest = { exerciseToDeleteIndex = null },
            title = { Text("Delete Exercise?") },
            text = {
                Text("Are you sure you want to remove $exerciseName from this workout?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        performedExercises = performedExercises
                            .filterIndexed { i, _ -> i != index }
                            .mapIndexed { i, ex -> ex.copy(orderIndex = i) }
                        exerciseToDeleteIndex = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDeleteIndex = null }) {
                    Text("Cancel")
                }
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
    onSave: (PerformedExercise) -> Unit,
    generatePerformedExerciseId: () -> String,
    generateSetId: () -> String
) {
    var selectedExerciseId by remember { mutableStateOf(existingExercise?.exerciseId ?: "") }

    // For new exercises, create sets with placeholder values that will be shown as empty
    // For existing exercises, use their actual values
    data class UiWorkoutSet(
        val id: String,
        val weight: String,
        val reps: String,
        val duration: String,
        val distance: String,
        val orderIndex: Int
    )

    var workoutSets by remember {
        mutableStateOf<List<UiWorkoutSet>>(
            if (existingExercise != null) {
                existingExercise.sets.map { set ->
                    UiWorkoutSet(
                        id = set.id,
                        weight = set.weightKg?.takeIf { it != 0f }?.toString() ?: "",
                        reps = set.reps?.takeIf { it != 0 }?.toString() ?: "",
                        duration = set.durationSeconds?.takeIf { it != 0 }?.toString() ?: "",
                        distance = set.distanceMeters?.takeIf { it != 0f }?.toString() ?: "",
                        orderIndex = set.orderIndex
                    )
                }
            } else {
                listOf(
                    UiWorkoutSet(
                        id = generateSetId(),
                        weight = "",
                        reps = "",
                        duration = "",
                        distance = "",
                        orderIndex = 0
                    )
                )
            }
        )
    }
    var expanded by remember { mutableStateOf(false) }
    var exerciseSearchQuery by remember { mutableStateOf("") }

    val selectedExercise = remember(selectedExerciseId, availableExercises) {
        availableExercises.find { it.id == selectedExerciseId }
    }
    val trackingType = selectedExercise?.trackingType ?: ExerciseTrackingType.WEIGHT_REPS

    val filteredExercises = remember(availableExercises, exerciseSearchQuery) {
        if (exerciseSearchQuery.isBlank()) {
            availableExercises
        } else {
            availableExercises.filter {
                it.name.contains(exerciseSearchQuery, ignoreCase = true)
            }
        }
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Search field (separate from dropdown)
                        OutlinedTextField(
                            value = exerciseSearchQuery,
                            onValueChange = { exerciseSearchQuery = it },
                            placeholder = { Text("Search exercises...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Exercise dropdown
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
                                if (filteredExercises.isEmpty()) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "No exercises found",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        onClick = { },
                                        enabled = false
                                    )
                                } else {
                                    filteredExercises.forEach { exercise ->
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
                                id = generateSetId(),
                                weight = workoutSets.lastOrNull()?.weight ?: "",
                                reps = workoutSets.lastOrNull()?.reps ?: "",
                                duration = workoutSets.lastOrNull()?.duration ?: "",
                                distance = workoutSets.lastOrNull()?.distance ?: "",
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
                                if (SetTrackingFields.showsReps(trackingType)) {
                                    OutlinedTextField(
                                        value = set.reps,
                                        onValueChange = { newReps ->
                                            val validated = InputValidation.validateReps(newReps)
                                            workoutSets = workoutSets.mapIndexed { i, s ->
                                                if (i == index) s.copy(reps = validated) else s
                                            }
                                        },
                                        label = { Text("Reps") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                                if (SetTrackingFields.showsWeight(trackingType)) {
                                    OutlinedTextField(
                                        value = set.weight,
                                        onValueChange = { newWeight ->
                                            InputValidation.validateWeight(newWeight)?.let { validated ->
                                                workoutSets = workoutSets.mapIndexed { i, s ->
                                                    if (i == index) s.copy(weight = validated) else s
                                                }
                                            }
                                        },
                                        label = { Text("kg") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                                if (SetTrackingFields.showsDuration(trackingType)) {
                                    OutlinedTextField(
                                        value = set.duration,
                                        onValueChange = { newDuration ->
                                            val validated = InputValidation.validateDuration(newDuration)
                                            workoutSets = workoutSets.mapIndexed { i, s ->
                                                if (i == index) s.copy(duration = validated) else s
                                            }
                                        },
                                        label = { Text("Duration (s)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                                if (SetTrackingFields.showsDistance(trackingType)) {
                                    OutlinedTextField(
                                        value = set.distance,
                                        onValueChange = { newDistance ->
                                            InputValidation.validateDistance(newDistance)?.let { validated ->
                                                workoutSets = workoutSets.mapIndexed { i, s ->
                                                    if (i == index) s.copy(distance = validated) else s
                                                }
                                            }
                                        },
                                        label = { Text("Distance (m)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
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
                    if (selectedExerciseId.isNotBlank() && workoutSets.isNotEmpty()) {
                        // Convert UI sets to domain model, filtering out empty sets.
                        // Which fields count depends on the exercise's tracking type,
                        // mirroring WorkoutSetValidator.
                        val domainSets = workoutSets
                            .filter {
                                SetTrackingFields.isFilled(trackingType, it.reps, it.duration, it.distance)
                            }
                            .mapIndexed { index, uiSet ->
                                WorkoutSet(
                                    id = uiSet.id,
                                    weightKg = if (SetTrackingFields.showsWeight(trackingType)) {
                                        uiSet.weight.toFloatOrNull()
                                    } else {
                                        null
                                    },
                                    reps = if (SetTrackingFields.showsReps(trackingType)) {
                                        uiSet.reps.toIntOrNull()
                                    } else {
                                        null
                                    },
                                    durationSeconds = if (SetTrackingFields.showsDuration(trackingType)) {
                                        uiSet.duration.toIntOrNull()
                                    } else {
                                        null
                                    },
                                    distanceMeters = if (SetTrackingFields.showsDistance(trackingType)) {
                                        uiSet.distance.toFloatOrNull()
                                    } else {
                                        null
                                    },
                                    isCompleted = true,
                                    orderIndex = index
                                )
                            }

                        if (domainSets.isNotEmpty()) {
                            // orderIndex is a placeholder - the caller re-stamps it based on
                            // final position in the performed exercises list. The exercise
                            // snapshot fields (name/category/primaryMuscles) are placeholders
                            // too - ValidateWorkoutSessionSetsUseCase resolves the real
                            // Exercise and stamps them before this is persisted.
                            // exerciseTrackingType is already the real, resolved value.
                            onSave(
                                PerformedExercise(
                                    id = existingExercise?.id ?: generatePerformedExerciseId(),
                                    exerciseId = selectedExerciseId,
                                    orderIndex = existingExercise?.orderIndex ?: 0,
                                    exerciseName = "",
                                    exerciseCategory = ExerciseCategory.STRENGTH,
                                    exerciseTrackingType = trackingType,
                                    exercisePrimaryMuscles = emptyList(),
                                    sets = domainSets
                                )
                            )
                        }
                    }
                },
                enabled = selectedExerciseId.isNotBlank() &&
                    workoutSets.any { SetTrackingFields.isFilled(trackingType, it.reps, it.duration, it.distance) }
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

/**
 * Get a human-readable label for a sort order.
 */
private fun getSortOrderLabel(sortOrder: WorkoutSessionSortOrder): String {
    return when (sortOrder) {
        WorkoutSessionSortOrder.DATE_NEWEST_FIRST -> "Date (Newest First)"
        WorkoutSessionSortOrder.DATE_OLDEST_FIRST -> "Date (Oldest First)"
        WorkoutSessionSortOrder.DURATION_LONGEST_FIRST -> "Duration (Longest First)"
        WorkoutSessionSortOrder.DURATION_SHORTEST_FIRST -> "Duration (Shortest First)"
        WorkoutSessionSortOrder.TITLE_A_TO_Z -> "Title (A-Z)"
        WorkoutSessionSortOrder.TITLE_Z_TO_A -> "Title (Z-A)"
    }
}
