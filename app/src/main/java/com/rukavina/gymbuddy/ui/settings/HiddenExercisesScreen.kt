package com.rukavina.gymbuddy.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rukavina.gymbuddy.data.model.Exercise
import com.rukavina.gymbuddy.ui.exercise.ExerciseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenExercisesScreen(
    navController: NavHostController,
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    val hiddenExercises by viewModel.hiddenExercises.collectAsState(initial = emptyList())
    var showUnhideAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hidden Exercises") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (hiddenExercises.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hidden exercises",
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Unhide All button
                Button(
                    onClick = { showUnhideAllDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unhide All Exercises")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // List of hidden exercises
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hiddenExercises) { exercise ->
                        HiddenExerciseItem(
                            exercise = exercise,
                            onUnhide = { viewModel.unhideExercise(exercise.id) }
                        )
                    }
                }
            }
        }
    }

    // Unhide All confirmation dialog
    if (showUnhideAllDialog) {
        AlertDialog(
            onDismissRequest = { showUnhideAllDialog = false },
            title = { Text("Unhide All Exercises?") },
            text = {
                Text("This will unhide all ${hiddenExercises.size} hidden exercises. They will appear in your exercise list again.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unhideAllExercises()
                    showUnhideAllDialog = false
                }) {
                    Text("Unhide All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnhideAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HiddenExerciseItem(
    exercise: Exercise,
    onUnhide: () -> Unit
) {
    var showUnhideDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = exercise.primaryMuscles.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(onClick = { showUnhideDialog = true }) {
                Icon(Icons.Default.Visibility, contentDescription = "Unhide")
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Unhide")
            }
        }
    }

    // Unhide confirmation dialog
    if (showUnhideDialog) {
        AlertDialog(
            onDismissRequest = { showUnhideDialog = false },
            title = { Text("Unhide Exercise?") },
            text = {
                Text("\"${exercise.name}\" will appear in your exercise list again.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onUnhide()
                    showUnhideDialog = false
                }) {
                    Text("Unhide")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnhideDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
