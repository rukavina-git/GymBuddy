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
import com.rukavina.gymbuddy.data.model.WorkoutTemplate
import com.rukavina.gymbuddy.ui.template.WorkoutTemplateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenTemplatesScreen(
    navController: NavHostController,
    viewModel: WorkoutTemplateViewModel = hiltViewModel()
) {
    val hiddenTemplates by viewModel.hiddenTemplates.collectAsState(initial = emptyList())
    var showRestoreAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hidden Templates") },
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
            if (hiddenTemplates.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hidden templates",
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Restore All button
                Button(
                    onClick = { showRestoreAllDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restore All Templates")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // List of hidden templates (no individual confirmation - just click to restore)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hiddenTemplates) { template ->
                        HiddenTemplateItem(
                            template = template,
                            onRestore = { viewModel.unhideTemplate(template.id) }
                        )
                    }
                }
            }
        }
    }

    // Restore All confirmation dialog
    if (showRestoreAllDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreAllDialog = false },
            title = { Text("Restore All Templates?") },
            text = {
                Text("This will restore all ${hiddenTemplates.size} hidden templates. They will appear in your template list again.")
            },
            confirmButton = {
                TextButton(onClick = {
                    hiddenTemplates.forEach { template ->
                        viewModel.unhideTemplate(template.id)
                    }
                    showRestoreAllDialog = false
                }) {
                    Text("Restore All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HiddenTemplateItem(
    template: WorkoutTemplate,
    onRestore: () -> Unit
) {
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
                    text = template.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${template.templateExercises.size} exercises",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(onClick = onRestore) {
                Icon(Icons.Default.Visibility, contentDescription = "Restore")
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Restore")
            }
        }
    }
}
