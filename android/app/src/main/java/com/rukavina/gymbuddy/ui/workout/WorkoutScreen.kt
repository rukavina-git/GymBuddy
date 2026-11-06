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
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.ui.exercises.ExercisesViewModel
import com.rukavina.gymbuddy.utils.UnitConverter
import com.rukavina.gymbuddy.utils.validation.InputValidation
import com.rukavina.gymbuddy.utils.validation.ValidationConstants
import java.text.SimpleDateFormat
import java.util.*

/**
 * Bridge until session detail (Plan 7 step 09): a past session opens the
 * old edit dialog, so it can still be fixed or deleted. Goes with 09.
 */
@Composable
fun SessionEditBridge(
    sessionId: String,
    onClose: () -> Unit,
    viewModel: WorkoutSessionViewModel = hiltViewModel(),
    exerciseViewModel: ExercisesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val exerciseUiState by exerciseViewModel.uiState.collectAsState()
    // Not loaded yet, or deleted meanwhile: nothing shows.
    val session = uiState.workoutSessions.firstOrNull { it.id == sessionId } ?: return
    WorkoutSessionFormDialog(
        workoutSession = session,
        availableExercises = exerciseUiState.rows,
        preferredUnits = uiState.preferredUnits,
        onDismiss = onClose,
        onSave = { draft ->
            viewModel.updateWorkoutSession(session, draft)
            onClose()
        },
        onDelete = {
            viewModel.deleteWorkoutSession(session.id)
            onClose()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionFormDialog(
    workoutSession: WorkoutSession?,
    availableExercises: List<Exercise>,
    preferredUnits: PreferredUnits,
    onDismiss: () -> Unit,
    onSave: (WorkoutSessionDraft) -> Unit,
    onDelete: (() -> Unit)? = null
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
        mutableStateOf(
            workoutSession?.performedExercises?.map { pe ->
                PerformedExerciseDraft(
                    existingId = pe.id,
                    exerciseId = pe.exerciseId,
                    exerciseName = pe.exerciseName,
                    exerciseTrackingType = pe.exerciseTrackingType,
                    orderIndex = pe.orderIndex,
                    sets = pe.sets.map { set ->
                        WorkoutSetDraft(
                            existingId = set.id,
                            weightKg = set.weightKg,
                            reps = set.reps,
                            durationSeconds = set.durationSeconds,
                            distanceMeters = set.distanceMeters,
                            orderIndex = set.orderIndex
                        )
                    }
                )
            } ?: emptyList()
        )
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
                            WorkoutSessionDraft(
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
            preferredUnits = preferredUnits,
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
    existingExercise: PerformedExerciseDraft?,
    preferredUnits: PreferredUnits,
    onDismiss: () -> Unit,
    onSave: (PerformedExerciseDraft) -> Unit
) {
    var selectedExerciseId by remember { mutableStateOf(existingExercise?.exerciseId ?: "") }

    // For new exercises, create sets with placeholder values that will be shown as empty
    // For existing exercises, use their actual values
    data class UiWorkoutSet(
        val existingId: String?,
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
                        existingId = set.existingId,
                        weight = set.weightKg?.takeIf { it != 0f }
                            ?.let { UnitConverter.weightToDisplayUnit(it, preferredUnits) } ?: "",
                        reps = set.reps?.takeIf { it != 0 }?.toString() ?: "",
                        duration = set.durationSeconds?.takeIf { it != 0 }?.toString() ?: "",
                        distance = set.distanceMeters?.takeIf { it != 0f }?.toString() ?: "",
                        orderIndex = set.orderIndex
                    )
                }
            } else {
                listOf(
                    UiWorkoutSet(
                        existingId = null,
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
                                existingId = null,
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
                                        label = { Text(UnitConverter.getWeightUnitLabel(preferredUnits)) },
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
                        // Convert UI sets to draft sets, filtering out empty sets.
                        // Which fields count depends on the exercise's tracking type,
                        // mirroring WorkoutSetValidator.
                        val draftSets = workoutSets
                            .filter {
                                SetTrackingFields.isFilled(trackingType, it.reps, it.duration, it.distance)
                            }
                            .mapIndexed { index, uiSet ->
                                WorkoutSetDraft(
                                    existingId = uiSet.existingId,
                                    weightKg = if (SetTrackingFields.showsWeight(trackingType)) {
                                        UnitConverter.weightToMetric(uiSet.weight, preferredUnits)
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
                                    orderIndex = index
                                )
                            }

                        if (draftSets.isNotEmpty()) {
                            // orderIndex is a placeholder - the caller re-stamps it based on
                            // final position in the performed exercises list. exerciseName is
                            // the resolved display name, used only for the in-progress list in
                            // WorkoutSessionFormDialog - ValidateWorkoutSessionSetsUseCase
                            // resolves the real Exercise and stamps the actual snapshot
                            // (name/category/primaryMuscles) before this is persisted.
                            // exerciseTrackingType is already the real, resolved value. No id
                            // is minted here - the ViewModel does that with its IdGenerator.
                            onSave(
                                PerformedExerciseDraft(
                                    existingId = existingExercise?.existingId,
                                    exerciseId = selectedExerciseId,
                                    exerciseName = selectedExercise?.name ?: "",
                                    exerciseTrackingType = trackingType,
                                    orderIndex = existingExercise?.orderIndex ?: 0,
                                    sets = draftSets
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
