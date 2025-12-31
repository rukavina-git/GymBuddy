package com.rukavina.gymbuddy.ui.exercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.navigation.NavHostController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rukavina.gymbuddy.data.model.Equipment
import com.rukavina.gymbuddy.data.model.Exercise
import com.rukavina.gymbuddy.data.model.MuscleGroup
import com.rukavina.gymbuddy.navigation.NavRoutes

/**
 * Exercise management screen with CRUD operations.
 * Shows list of exercises and allows create, edit, delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreen(
    navController: NavHostController,
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<Exercise?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedMuscles by remember { mutableStateOf<Set<MuscleGroup>>(emptySet()) }
    var selectedEquipment by remember { mutableStateOf<Set<Equipment>>(emptySet()) }

    Scaffold(
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
                    // Filter exercises based on search and filters (AND logic for multiple filters)
                    val filteredExercises = uiState.exercises.filter { exercise ->
                        val matchesSearch = searchQuery.isBlank() ||
                            exercise.name.contains(searchQuery, ignoreCase = true) ||
                            exercise.description?.contains(searchQuery, ignoreCase = true) == true ||
                            exercise.primaryMuscles.any { it.name.contains(searchQuery, ignoreCase = true) }

                        // Must have ALL selected muscle groups (AND logic)
                        val matchesMuscles = selectedMuscles.isEmpty() ||
                            selectedMuscles.all { selectedMuscle ->
                                exercise.primaryMuscles.contains(selectedMuscle) ||
                                exercise.secondaryMuscles.contains(selectedMuscle)
                            }

                        // Must have ALL selected equipment (AND logic)
                        val matchesEquipment = selectedEquipment.isEmpty() ||
                            selectedEquipment.all { selectedEquip ->
                                exercise.equipmentNeeded.contains(selectedEquip)
                            }

                        matchesSearch && matchesMuscles && matchesEquipment
                    }

                    val hasActiveFilters = selectedMuscles.isNotEmpty() || selectedEquipment.isNotEmpty()

                    // Group exercises by first letter
                    val groupedExercises = filteredExercises
                        .sortedBy { it.name.lowercase() }
                        .groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Clean header with title and search icon
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EXERCISES",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            IconButton(onClick = { showSearchDialog = true }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Filter button with clear option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = hasActiveFilters,
                                onClick = { showFilterSheet = true },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Filter",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            if (hasActiveFilters)
                                                "Filtered (${filteredExercises.size})"
                                            else
                                                "All (${filteredExercises.size})"
                                        )
                                    }
                                }
                            )

                            // Clear filters button (only show when filters are active)
                            if (hasActiveFilters) {
                                IconButton(
                                    onClick = {
                                        selectedMuscles = emptySet()
                                        selectedEquipment = emptySet()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear filters",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Exercise list grouped by letter
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
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                groupedExercises.forEach { (letter, exercises) ->
                                    // Letter header
                                    item {
                                        Text(
                                            text = letter.toString(),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Exercises in this group
                                    items(exercises) { exercise ->
                                        SimpleExerciseItem(
                                            exercise = exercise,
                                            onClick = {
                                                navController.navigate(NavRoutes.exerciseDetailsRoute(exercise.id))
                                            }
                                        )
                                    }
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

        // Search Dialog
        if (showSearchDialog) {
            BasicAlertDialog(
                onDismissRequest = { showSearchDialog = false }
            ) {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Search Exercises",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search by name...") },
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showSearchDialog = false }) {
                                Text("Done")
                            }
                        }
                    }
                }
            }
        }

        // Filter Bottom Sheet
        if (showFilterSheet) {
            ExerciseFilterBottomSheet(
                selectedMuscles = selectedMuscles,
                selectedEquipment = selectedEquipment,
                totalExercises = uiState.exercises.size,
                onDismiss = { showFilterSheet = false },
                onApplyFilters = { muscles, equipment ->
                    selectedMuscles = muscles
                    selectedEquipment = equipment
                    showFilterSheet = false
                }
            )
        }
    }
}

@Composable
fun SimpleExerciseItem(
    exercise: Exercise,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail
            Surface(
                modifier = Modifier.size(60.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (exercise.thumbnailUrl != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(exercise.thumbnailUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = exercise.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Exercise name and muscles
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
                Text(
                    text = exercise.primaryMuscles.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseFilterBottomSheet(
    selectedMuscles: Set<MuscleGroup>,
    selectedEquipment: Set<Equipment>,
    totalExercises: Int,
    onDismiss: () -> Unit,
    onApplyFilters: (Set<MuscleGroup>, Set<Equipment>) -> Unit
) {
    var tempSelectedMuscles by remember { mutableStateOf(selectedMuscles) }
    var tempSelectedEquipment by remember { mutableStateOf(selectedEquipment) }

    // Start with skip=true to force initial Expanded state, then allow partial
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 80.dp) // Space for action buttons
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Exercises",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = "$totalExercises Total",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Filter content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    // Muscle Groups section
                    Text(
                        text = "Muscle Groups (${if (tempSelectedMuscles.isEmpty()) "All" else tempSelectedMuscles.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MuscleGroup.entries.forEach { muscle ->
                            FilterChip(
                                selected = tempSelectedMuscles.contains(muscle),
                                onClick = {
                                    tempSelectedMuscles = if (tempSelectedMuscles.contains(muscle)) {
                                        tempSelectedMuscles - muscle
                                    } else {
                                        tempSelectedMuscles + muscle
                                    }
                                },
                                label = { Text(muscle.name.replace("_", " ")) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Equipment section
                    Text(
                        text = "Equipment (${if (tempSelectedEquipment.isEmpty()) "All" else tempSelectedEquipment.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Equipment.entries.forEach { equipment ->
                            FilterChip(
                                selected = tempSelectedEquipment.contains(equipment),
                                onClick = {
                                    tempSelectedEquipment = if (tempSelectedEquipment.contains(equipment)) {
                                        tempSelectedEquipment - equipment
                                    } else {
                                        tempSelectedEquipment + equipment
                                    }
                                },
                                label = {
                                    Text(
                                        equipment.name.replace("_", " ").lowercase()
                                            .replaceFirstChar { it.uppercase() }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Action buttons at bottom
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clear all button
                    OutlinedButton(
                        onClick = {
                            tempSelectedMuscles = emptySet()
                            tempSelectedEquipment = emptySet()
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Clear All")
                    }

                    // Apply button
                    Button(
                        onClick = { onApplyFilters(tempSelectedMuscles, tempSelectedEquipment) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}
