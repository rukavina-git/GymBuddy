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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.TemplateExercise
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutViewModel

/**
 * Old create/edit dialog, kept until 05b replaces it with the editor
 * screen (Figma Editor A). Opened from the template page pencil and
 * "+ New template".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTemplateFormDialog(
    template: WorkoutTemplate?,
    availableExercises: List<Exercise>,
    onDismiss: () -> Unit,
    onSave: (WorkoutTemplateDraft) -> Unit
) {
    // Form state
    var title by remember { mutableStateOf(template?.title ?: "") }
    var nextLocalKey by remember { mutableIntStateOf(template?.templateExercises?.size ?: 0) }
    var templateExercises by remember {
        mutableStateOf(
            template?.templateExercises?.mapIndexed { index, ex ->
                TemplateExerciseDraft(
                    localKey = index,
                    existingId = ex.id,
                    exerciseId = ex.exerciseId,
                    exerciseName = ex.exerciseName,
                    exerciseTrackingType = ex.exerciseTrackingType,
                    plannedSets = ex.plannedSets,
                    plannedReps = ex.plannedReps,
                    orderIndex = ex.orderIndex,
                    restSeconds = ex.restSeconds,
                    notes = ex.notes
                )
            } ?: emptyList()
        )
    }
    var isTitleEditing by remember { mutableStateOf(template == null) } // Auto-edit for new templates

    // Dialog states
    var showExercisePicker by remember { mutableStateOf(false) }
    var editingExerciseIndex by remember { mutableStateOf<Int?>(null) }
    var showExerciseConfig by remember { mutableStateOf(false) }
    var pendingExerciseId by remember { mutableStateOf<String?>(null) }
    var exerciseToDelete by remember { mutableStateOf<TemplateExerciseDraft?>(null) }

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
                            key = { it.localKey }
                        ) { exercise ->
                            TemplateExerciseListItem(
                                exerciseName = exercise.exerciseName.ifBlank { "Unknown" },
                                exerciseTrackingType = exercise.exerciseTrackingType,
                                plannedSets = exercise.plannedSets,
                                plannedReps = exercise.plannedReps,
                                plannedDurationSeconds = null,
                                plannedDistanceMeters = null,
                                restSeconds = exercise.restSeconds,
                                position = templateExercises.indexOf(exercise),
                                totalCount = templateExercises.size,
                                onMoveUp = {
                                    templateExercises = reorderExercise(
                                        templateExercises,
                                        exercise.localKey,
                                        -1
                                    )
                                },
                                onMoveDown = {
                                    templateExercises = reorderExercise(
                                        templateExercises,
                                        exercise.localKey,
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
                        WorkoutTemplateDraft(
                            title = title,
                            exercises = templateExercises.sortedBy { it.orderIndex }
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
            exerciseId = pendingExerciseId ?: editingExercise?.exerciseId ?: "",
            // editingExercise already carries its stamped snapshot; a brand
            // new pick (pendingExerciseId) has no TemplateExercise yet, so
            // that case still needs the live library lookup.
            exerciseName = editingExercise?.exerciseName ?: exerciseMap[pendingExerciseId]?.name ?: "",
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
                    // Add new. No id is minted here - the ViewModel does that
                    // with its IdGenerator. exerciseName is the resolved
                    // display name, used only for this in-progress list -
                    // StampTemplateExerciseSnapshotsUseCase resolves the real
                    // Exercise and stamps the actual snapshot (including
                    // exerciseTrackingType) before this is persisted.
                    val newExercise = TemplateExerciseDraft(
                        localKey = nextLocalKey++,
                        existingId = null,
                        exerciseId = pendingExerciseId!!,
                        exerciseName = exerciseMap[pendingExerciseId]?.name ?: "",
                        exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
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
        val exerciseName = exercise.exerciseName.ifBlank { "this exercise" }
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text("Remove Exercise?") },
            text = { Text("Are you sure you want to remove \"$exerciseName\" from this template?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        templateExercises = templateExercises
                            .filter { it.localKey != exercise.localKey }
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
 * Helper function for reordering exercise drafts in a template being edited.
 * @param exercises The current list of template exercise drafts
 * @param localKey The Compose-only list key of the draft to move
 * @param direction -1 for up, +1 for down
 * @return Updated list with reordered exercises and updated orderIndex values
 */
private fun reorderExercise(
    exercises: List<TemplateExerciseDraft>,
    localKey: Int,
    direction: Int
): List<TemplateExerciseDraft> {
    val sorted = exercises.sortedBy { it.orderIndex }
    val currentIndex = sorted.indexOfFirst { it.localKey == localKey }
    val newIndex = (currentIndex + direction).coerceIn(0, sorted.size - 1)

    if (currentIndex == newIndex) return exercises

    val mutable = sorted.toMutableList()
    val item = mutable.removeAt(currentIndex)
    mutable.add(newIndex, item)

    return mutable.mapIndexed { index, exercise ->
        exercise.copy(orderIndex = index)
    }
}

/**
 * Formats a planned target measurement, branching on exerciseTrackingType
 * so the right field (reps, duration, distance) is shown instead of a
 * hardcoded "reps" that reads "null" for exercises with no rep target
 * (e.g. a DURATION exercise like Plank).
 */
internal fun formatPlannedTarget(
    exerciseTrackingType: ExerciseTrackingType,
    plannedReps: Int?,
    plannedDurationSeconds: Int?,
    plannedDistanceMeters: Float?
): String {
    return when (exerciseTrackingType) {
        ExerciseTrackingType.WEIGHT_REPS, ExerciseTrackingType.REPS_ONLY ->
            plannedReps?.toString() ?: "-"

        ExerciseTrackingType.DURATION, ExerciseTrackingType.WEIGHT_DURATION ->
            plannedDurationSeconds?.let { "${it}s" } ?: "-"

        ExerciseTrackingType.DISTANCE_DURATION, ExerciseTrackingType.WEIGHT_DISTANCE ->
            plannedDistanceMeters?.let { "${it.toInt()}m" } ?: "-"
    }
}

/**
 * Convenience overload for a real TemplateExercise (loaded from the
 * database) - see the primitive overload above for the formatting rules.
 */
internal fun formatPlannedTarget(templateExercise: TemplateExercise): String =
    formatPlannedTarget(
        templateExercise.exerciseTrackingType,
        templateExercise.plannedReps,
        templateExercise.plannedDurationSeconds,
        templateExercise.plannedDistanceMeters
    )
