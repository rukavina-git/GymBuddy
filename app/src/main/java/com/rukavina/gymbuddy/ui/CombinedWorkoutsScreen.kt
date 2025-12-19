package com.rukavina.gymbuddy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.rukavina.gymbuddy.ui.template.WorkoutTemplateScreen
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutViewModel
import com.rukavina.gymbuddy.ui.workout.WorkoutScreen

/**
 * Combined screen that displays both Templates and Sessions
 * with top tabs to switch between them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombinedWorkoutsScreen(
    activeWorkoutViewModel: ActiveWorkoutViewModel = hiltViewModel(),
    onStartWorkout: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Templates", "Sessions")

    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar with title
        TopAppBar(
            title = { Text("Workouts") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

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
