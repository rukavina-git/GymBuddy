package com.rukavina.gymbuddy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutUiState
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    activeWorkoutViewModel: ActiveWorkoutViewModel? = null,
    workoutSessionViewModel: com.rukavina.gymbuddy.ui.workout.WorkoutSessionViewModel = hiltViewModel(),
    onNavigateToWorkout: () -> Unit = {},
    onNavigateToTemplates: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState = activeWorkoutViewModel?.uiState?.collectAsState()
    val userName = "Alex"
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Get workout sessions from the view model
    val workoutSessionState by workoutSessionViewModel.uiState.collectAsState()
    val workoutSessions = workoutSessionState.workoutSessions

    // Convert workout dates to LocalDate - this will update when workoutSessions changes
    val workoutDates = remember(workoutSessions) {
        workoutSessions.map { session ->
            Instant.ofEpochMilli(session.date)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }.distinct()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Welcome Section
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                WelcomeSection(
                    userName = userName,
                    onProfileClick = onNavigateToProfile
                )
            }

            // 2. Current Workout Card
            if (uiState != null) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    CurrentWorkoutCard(
                        activeWorkoutViewModel = activeWorkoutViewModel,
                        uiState = uiState.value,
                        onNavigateToWorkout = onNavigateToWorkout,
                        onNavigateToTemplates = onNavigateToTemplates
                    )
                }
            }

            // 3. Motivation Quote
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                MotivationQuoteCard()
            }

            // 4. Workout Calendar
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                WorkoutCalendar(
                    workoutDates = workoutDates,
                    onDateClick = { date ->
                        val hasWorkout = workoutDates.contains(date)
                        val dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy")
                        val formattedDate = date.format(dateFormatter)
                        val message = if (hasWorkout) {
                            "✓ Workout on $formattedDate"
                        } else {
                            "✗ No workout on $formattedDate"
                        }
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
                    }
                )
            }

            // 5. Bottom section - reserved for future
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WelcomeSection(
    userName: String,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Welcome back,",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = userName,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun CurrentWorkoutCard(
    activeWorkoutViewModel: ActiveWorkoutViewModel,
    uiState: ActiveWorkoutUiState,
    onNavigateToWorkout: () -> Unit,
    onNavigateToTemplates: () -> Unit
) {
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
                        if (hasActiveWorkout) uiState.workoutTitle else "Choose a template to begin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (hasActiveWorkout) {
                val timerText = remember(uiState.elapsedSeconds) {
                    val hours = uiState.elapsedSeconds / 3600
                    val minutes = (uiState.elapsedSeconds % 3600) / 60
                    val seconds = uiState.elapsedSeconds % 60
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
}

@Composable
private fun MotivationQuoteCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "\"The only bad workout is the one that didn't happen.\"",
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "— Unknown",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun WorkoutCalendar(
    workoutDates: List<LocalDate>,
    onDateClick: (LocalDate) -> Unit
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(12) }
    val endMonth = remember { currentMonth.plusMonths(12) }
    val daysOfWeek = remember { daysOfWeek() }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first()
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Workout Calendar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val visibleMonth = state.firstVisibleMonth.yearMonth
                Text(
                    text = visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " ${visibleMonth.year}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Days of week header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                daysOfWeek.forEach { dayOfWeek ->
                    Text(
                        text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Calendar
            HorizontalCalendar(
                state = state,
                dayContent = { day ->
                    Day(
                        day = day,
                        isWorkoutDay = workoutDates.contains(day.date),
                        onClick = { onDateClick(day.date) }
                    )
                }
            )
        }
    }
}

@Composable
private fun Day(
    day: CalendarDay,
    isWorkoutDay: Boolean,
    onClick: () -> Unit
) {
    val isToday = day.date == LocalDate.now()
    val isCurrentMonth = day.position == DayPosition.MonthDate

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isWorkoutDay && isCurrentMonth -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    isToday && isCurrentMonth -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(
                enabled = isCurrentMonth,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                !isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                isWorkoutDay -> MaterialTheme.colorScheme.primary
                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (isWorkoutDay || isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}