package com.rukavina.gymbuddy.ui.settings.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Debug builds only. Plain Material, no Figma frame (dev-only tool). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDebugScreen(
    navController: NavHostController,
    viewModel: SyncDebugViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync debug") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = viewModel::syncNow, enabled = !state.isSyncing) { Text("Sync now") }
                    if (state.isSyncing) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
                if (state.signedOut) Text("Signed out, no sync ran.", color = MaterialTheme.colorScheme.error)
            }

            item {
                DebugSection("Last sync") {
                    val status = state.status
                    DebugLine("Time", status.lastSyncAt?.let(::formatTime) ?: "none since app start")
                    DebugLine("Outcome", status.lastOutcome?.name ?: "-")
                    status.lastError?.let { DebugLine("Error", it) }
                    status.lastTraceId?.let { DebugLine("Trace id", it) }
                    DebugLine("Cursor", state.cursor ?: "none")
                }
            }

            item {
                DebugSection("Outbox (${state.outbox.size})") {
                    if (state.outbox.isEmpty()) Text("Empty")
                    state.outboxCounts.forEach { (type, count) -> DebugLine(type.name, count.toString()) }
                }
            }
            items(state.outbox, key = { "${it.entityType}/${it.entityId}" }) { entry ->
                Text(
                    "${entry.entityType} ${entry.operation} v${entry.changeVersion}\n${entry.entityId}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }

            item {
                DebugSection("Discarded changes (${state.discarded.size})") {
                    if (state.discarded.isEmpty()) Text("None")
                }
            }
            items(state.discarded, key = { it.id }) { log ->
                Text(
                    "${formatTime(log.loggedAt)} ${log.outcome} ${log.entityType}\n${log.entityId}" +
                        (log.reason?.let { "\n$it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun DebugSection(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun DebugLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
    }
}

private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

private fun formatTime(epochMs: Long): String =
    timeFormat.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
