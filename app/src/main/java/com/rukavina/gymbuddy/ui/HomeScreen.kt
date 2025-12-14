package com.rukavina.gymbuddy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    activeWorkoutViewModel: ActiveWorkoutViewModel? = null,
    onNavigateToWorkout: () -> Unit = {},
    onNavigateToTemplates: () -> Unit = {}
) {
    val uiState = activeWorkoutViewModel?.uiState?.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Home") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Workout Banner - Active or Start New
            if (uiState != null) {
                val hasActiveWorkout = activeWorkoutViewModel.hasActiveWorkout()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (hasActiveWorkout) {
                                onNavigateToWorkout()
                            } else {
                                onNavigateToTemplates()
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = if (hasActiveWorkout) "Active Workout" else "Start Workout",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    if (hasActiveWorkout) "Active Workout" else "Start Workout",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    if (hasActiveWorkout) uiState.value.workoutTitle else "Choose a template to begin",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Timer display (only if active workout)
                        if (hasActiveWorkout) {
                            val timerText = remember(uiState.value.elapsedSeconds) {
                                val hours = uiState.value.elapsedSeconds / 3600
                                val minutes = (uiState.value.elapsedSeconds % 3600) / 60
                                val seconds = uiState.value.elapsedSeconds % 60
                                String.format("%d:%02d:%02d", hours, minutes, seconds)
                            }

                            Text(
                                text = timerText,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}