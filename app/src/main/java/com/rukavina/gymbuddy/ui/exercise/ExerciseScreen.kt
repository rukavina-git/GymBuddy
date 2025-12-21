package com.rukavina.gymbuddy.ui.exercise

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    var searchQuery by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf<DifficultyLevel?>(null) }
    var selectedMuscle by remember { mutableStateOf<MuscleGroup?>(null) }

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
                    // Filter exercises based on search and filters
                    val filteredExercises = uiState.exercises.filter { exercise ->
                        val matchesSearch = searchQuery.isBlank() ||
                            exercise.name.contains(searchQuery, ignoreCase = true) ||
                            exercise.description?.contains(searchQuery, ignoreCase = true) == true ||
                            exercise.primaryMuscles.any { it.name.contains(searchQuery, ignoreCase = true) }

                        val matchesDifficulty = selectedDifficulty == null || exercise.difficulty == selectedDifficulty
                        val matchesMuscle = selectedMuscle == null ||
                            exercise.primaryMuscles.contains(selectedMuscle) ||
                            exercise.secondaryMuscles.contains(selectedMuscle)

                        matchesSearch && matchesDifficulty && matchesMuscle
                    }

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            placeholder = { Text("Search exercises...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true
                        )

                        // Filter chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Difficulty filter
                            var showDifficultyMenu by remember { mutableStateOf(false) }
                            Box {
                                FilterChip(
                                    selected = selectedDifficulty != null,
                                    onClick = { showDifficultyMenu = true },
                                    label = {
                                        Text(selectedDifficulty?.name ?: "Difficulty")
                                    },
                                    trailingIcon = {
                                        if (selectedDifficulty != null) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                modifier = Modifier.clickable { selectedDifficulty = null }
                                            )
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = showDifficultyMenu,
                                    onDismissRequest = { showDifficultyMenu = false }
                                ) {
                                    DifficultyLevel.entries.forEach { difficulty ->
                                        DropdownMenuItem(
                                            text = { Text(difficulty.name) },
                                            onClick = {
                                                selectedDifficulty = difficulty
                                                showDifficultyMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Muscle group filter
                            var showMuscleMenu by remember { mutableStateOf(false) }
                            Box {
                                FilterChip(
                                    selected = selectedMuscle != null,
                                    onClick = { showMuscleMenu = true },
                                    label = {
                                        Text(selectedMuscle?.name ?: "Muscle")
                                    },
                                    trailingIcon = {
                                        if (selectedMuscle != null) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                modifier = Modifier.clickable { selectedMuscle = null }
                                            )
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = showMuscleMenu,
                                    onDismissRequest = { showMuscleMenu = false }
                                ) {
                                    MuscleGroup.entries.forEach { muscle ->
                                        DropdownMenuItem(
                                            text = { Text(muscle.name) },
                                            onClick = {
                                                selectedMuscle = muscle
                                                showMuscleMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Exercise list
                        if (filteredExercises.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No exercises match your filters",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredExercises) { exercise ->
                                    ExerciseItem(
                                        exercise = exercise,
                                        onEdit = {
                                            editingExercise = exercise
                                            showDialog = true
                                        },
                                        onDelete = {
                                            viewModel.deleteExercise(exercise.id)
                                        },
                                        onHide = { id ->
                                            viewModel.hideExercise(id)
                                        }
                                    )
                                }
                            }
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
            ExerciseWizardDialog(
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
    onDelete: () -> Unit,
    onHide: ((Int) -> Unit)? = null
) {
    var showDetails by remember { mutableStateOf(false) }
    var showHideConfirmation by remember { mutableStateOf(false) }

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
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Row {
                    if (exercise.isCustom) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        // Hide button for default exercises
                        IconButton(onClick = { showHideConfirmation = true }) {
                            Icon(Icons.Default.VisibilityOff, "Hide", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Difficulty badge
            AssistChip(
                onClick = { },
                label = {
                    Text(
                        exercise.difficulty.name,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )

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

    // Hide confirmation dialog
    if (showHideConfirmation) {
        AlertDialog(
            onDismissRequest = { showHideConfirmation = false },
            title = { Text("Hide Exercise?") },
            text = {
                Text("This will hide \"${exercise.name}\" from your exercise list. You can unhide it anytime in Settings.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onHide?.invoke(exercise.id)
                    showHideConfirmation = false
                }) {
                    Text("Hide")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHideConfirmation = false }) {
                    Text("Cancel")
                }
            }
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
                    singleLine = true,
                    enabled = exercise?.isCustom != false
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
                                id = exercise?.id ?: 0,
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
                                createdBy = exercise?.createdBy,
                                isHidden = exercise?.isHidden ?: false
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
    val context = LocalContext.current

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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    // Chips row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SuggestionChip(
                            onClick = { },
                            label = { Text(exercise.difficulty.name) }
                        )
                        SuggestionChip(
                            onClick = { },
                            label = { Text(exercise.exerciseType.name) }
                        )
                    }

                    // Watch Video Button
                    exercise.videoUrl?.let { videoUrl ->
                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Watch Video")
                        }
                    }
                }

                // Content sections
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Description
                    exercise.description?.let { desc ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "About",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Instructions
                    if (exercise.instructions.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "How to Perform",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            exercise.instructions.forEachIndexed { index, instruction ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Text(
                                        text = instruction,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Muscle Groups
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Muscles",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Primary",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                exercise.primaryMuscles.forEach { muscle ->
                                    Text(
                                        text = muscle.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            if (exercise.secondaryMuscles.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Secondary",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    exercise.secondaryMuscles.forEach { muscle ->
                                        Text(
                                            text = muscle.name,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Equipment
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Equipment",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = exercise.equipmentNeeded.joinToString(", ") {
                                it.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() }
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Close button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
