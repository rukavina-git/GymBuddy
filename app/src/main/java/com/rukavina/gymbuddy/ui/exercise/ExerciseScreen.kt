package com.rukavina.gymbuddy.ui.exercise

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.rukavina.gymbuddy.navigation.NavRoutes
import com.rukavina.gymbuddy.ui.components.EmptyState
import com.rukavina.gymbuddy.ui.components.FilterBottomSheet
import com.rukavina.gymbuddy.ui.components.LoadingState
import com.rukavina.gymbuddy.ui.components.ScreenHeader
import com.rukavina.gymbuddy.ui.components.ThumbnailCard
import com.rukavina.gymbuddy.ui.exercise.components.ExerciseFilterContent

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
    var editingExercise by remember { mutableStateOf<com.rukavina.gymbuddy.data.model.Exercise?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var exerciseToDelete by remember { mutableStateOf<com.rukavina.gymbuddy.data.model.Exercise?>(null) }
    var exerciseToHide by remember { mutableStateOf<com.rukavina.gymbuddy.data.model.Exercise?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingExercise = null
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
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
                    LoadingState()
                }
                uiState.exercises.isEmpty() -> {
                    EmptyState(
                        message = "No exercises yet. Tap + to add one!"
                    )
                }
                else -> {
                    val filteredExercises = uiState.filteredExercises
                    val hasActiveFilters = uiState.selectedMuscles.isNotEmpty() ||
                                          uiState.selectedEquipment.isNotEmpty()

                    // Group exercises by first letter
                    val groupedExercises = filteredExercises
                        .sortedBy { it.name.lowercase() }
                        .groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Expandable search header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSearchExpanded) {
                                // Search mode - show text field
                                OutlinedTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = { viewModel.updateSearchQuery(it) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester),
                                    placeholder = { Text("Search exercises...") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            isSearchExpanded = false
                                            viewModel.updateSearchQuery("")
                                        }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Close search")
                                        }
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                // Auto-focus when expanded
                                LaunchedEffect(Unit) {
                                    focusRequester.requestFocus()
                                }
                            } else {
                                // Normal mode - show title and search icon
                                ScreenHeader(
                                    title = "EXERCISES"
                                )
                                IconButton(onClick = { isSearchExpanded = true }) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
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
                                    onClick = { viewModel.clearAllFilters() },
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
                            EmptyState(
                                message = "No exercises match your filters"
                            )
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
                                        ThumbnailCard(
                                            title = exercise.name,
                                            subtitle = exercise.primaryMuscles.joinToString(", ") { it.name },
                                            thumbnailUrl = exercise.thumbnailUrl,
                                            onClick = {
                                                navController.navigate(NavRoutes.exerciseDetailsRoute(exercise.id))
                                            },
                                            trailingContent = {
                                                Row {
                                                    if (exercise.isCustom) {
                                                        // Custom exercises can be edited and deleted
                                                        IconButton(onClick = {
                                                            editingExercise = exercise
                                                            showDialog = true
                                                        }) {
                                                            Icon(
                                                                Icons.Default.Edit,
                                                                contentDescription = "Edit",
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                        IconButton(onClick = {
                                                            exerciseToDelete = exercise
                                                        }) {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                contentDescription = "Delete",
                                                                tint = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                    } else {
                                                        // Default exercises can only be hidden
                                                        IconButton(onClick = {
                                                            exerciseToHide = exercise
                                                        }) {
                                                            Icon(
                                                                Icons.Default.VisibilityOff,
                                                                contentDescription = "Hide",
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
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

        // Create/Edit Bottom Sheet
        if (showDialog) {
            ExerciseCreationBottomSheet(
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

        // Filter Bottom Sheet
        if (showFilterSheet) {
            var tempMuscles by remember { mutableStateOf(uiState.selectedMuscles) }
            var tempEquipment by remember { mutableStateOf(uiState.selectedEquipment) }

            FilterBottomSheet(
                title = "Filter Exercises",
                totalItemsCount = uiState.exercises.size,
                onDismiss = { showFilterSheet = false },
                onApply = {
                    viewModel.updateMuscleFilters(tempMuscles)
                    viewModel.updateEquipmentFilters(tempEquipment)
                    showFilterSheet = false
                },
                onClearAll = {
                    tempMuscles = emptySet()
                    tempEquipment = emptySet()
                }
            ) {
                ExerciseFilterContent(
                    selectedMuscles = tempMuscles,
                    selectedEquipment = tempEquipment,
                    onMusclesChange = { tempMuscles = it },
                    onEquipmentChange = { tempEquipment = it }
                )
            }
        }

        // Delete exercise confirmation dialog
        exerciseToDelete?.let { exercise ->
            AlertDialog(
                onDismissRequest = { exerciseToDelete = null },
                title = { Text("Delete Exercise?") },
                text = {
                    Text("Are you sure you want to delete \"${exercise.name}\"? This action cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteExercise(exercise.id)
                            exerciseToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { exerciseToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Hide exercise confirmation dialog
        exerciseToHide?.let { exercise ->
            AlertDialog(
                onDismissRequest = { exerciseToHide = null },
                title = { Text("Hide Exercise?") },
                text = {
                    Text("\"${exercise.name}\" will be hidden from your exercise list. You can restore it from App Preferences.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.hideExercise(exercise.id)
                            exerciseToHide = null
                        }
                    ) {
                        Text("Hide")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { exerciseToHide = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
