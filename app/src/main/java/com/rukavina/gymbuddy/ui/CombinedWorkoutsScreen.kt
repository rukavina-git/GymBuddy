package com.rukavina.gymbuddy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rukavina.gymbuddy.ui.components.ScreenHeader
import com.rukavina.gymbuddy.ui.template.WorkoutTemplateScreen
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutViewModel
import com.rukavina.gymbuddy.ui.workout.WorkoutScreen

/**
 * Combined screen that displays both Templates and Sessions
 * with top tabs to switch between them.
 */
@Composable
fun CombinedWorkoutsScreen(
    activeWorkoutViewModel: ActiveWorkoutViewModel = hiltViewModel(),
    onStartWorkout: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Templates", "Sessions")

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                ScreenHeader(
                    title = "WORKOUTS"
                )
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> WorkoutTemplateScreen(
                    activeWorkoutViewModel = activeWorkoutViewModel,
                    onStartWorkout = onStartWorkout
                )
                1 -> WorkoutScreen()
            }
        }
    }
}
