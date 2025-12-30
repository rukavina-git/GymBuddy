package com.rukavina.gymbuddy.ui.exercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.rukavina.gymbuddy.data.model.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailsScreen(
    exercise: Exercise,
    onNavigateBack: () -> Unit,
    onUpdateNote: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("INFO", "HISTORY")

    // Set status bar for white background with dark icons
    val systemUiController = rememberSystemUiController()

    DisposableEffect(systemUiController) {
        // Set status bar to white with dark icons for visibility
        systemUiController.setStatusBarColor(
            color = Color.White,
            darkIcons = true
        )
        // Set navigation bar to match
        systemUiController.setNavigationBarColor(
            color = Color.White,
            darkIcons = true
        )

        onDispose {
            // Reset to transparent when leaving screen
            systemUiController.setStatusBarColor(
                color = Color.Transparent,
                darkIcons = true
            )
            systemUiController.setNavigationBarColor(
                color = Color.Transparent,
                darkIcons = true
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab content
            when (selectedTab) {
                0 -> InfoTab(exercise, onUpdateNote)
                1 -> HistoryTab()
            }
        }

        // Floating DONE button at bottom
        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                text = "DONE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun InfoTab(
    exercise: Exercise,
    onUpdateNote: (String) -> Unit
) {
    val context = LocalContext.current
    var noteText by remember { mutableStateOf(exercise.note ?: "") }
    var isEditingNote by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 80.dp) // Space for the DONE button
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Exercise name FIRST
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Thumbnail/Video area SECOND
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant
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
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Error loading image",
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Failed to load preview",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play video",
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Text(
                            text = "No preview available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Modern note field
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isEditingNote = true },
            colors = CardDefaults.cardColors(
                containerColor = if (isEditingNote)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    MaterialTheme.colorScheme.surface
            ),
            border = if (isEditingNote)
                null
            else
                CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Personal Notes",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isEditingNote) {
                        TextButton(
                            onClick = {
                                onUpdateNote(noteText)
                                isEditingNote = false
                            }
                        ) {
                            Text("Done")
                        }
                    }
                }

                if (isEditingNote) {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Add your personal notes here...") },
                        minLines = 2,
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                } else {
                    Text(
                        text = noteText.ifBlank { "Tap to add notes" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (noteText.isBlank())
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Muscle Groups (Primary and Secondary muscles)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "MUSCLE GROUPS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(120.dp)
                )
                Text(
                    text = buildString {
                        append(exercise.primaryMuscles.joinToString(", ") { it.name })
                        if (exercise.secondaryMuscles.isNotEmpty()) {
                            append(", ")
                            append(exercise.secondaryMuscles.joinToString(", ") { it.name })
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Equipment
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "EQUIPMENT",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(120.dp)
                )
                Text(
                    text = exercise.equipmentNeeded.joinToString(", ") {
                        it.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // DESCRIPTION section
        if (exercise.description?.isNotBlank() == true) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "DESCRIPTION",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = exercise.description ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.times(1.5f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // INSTRUCTIONS section
        if (exercise.instructions.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "INSTRUCTIONS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                exercise.instructions.forEachIndexed { index, instruction ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(
                            text = instruction,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.times(1.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // KEY TIPS section (at the end)
        if (exercise.tips.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "KEY TIPS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                exercise.tips.forEachIndexed { index, tip ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.times(1.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HistoryTab() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "History will be shown here",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Coming soon...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
