package com.rukavina.gymbuddy.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
