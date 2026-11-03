package com.rukavina.gymbuddy.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.data.sync.LocalDataCounts
import com.rukavina.gymbuddy.ui.guest.countsText

/**
 * Export sheet, plain until Plan 7.10. Structure from Figma R15-11
 * (node 110:18444) without the checkboxes: CSV was dropped (ADR 0015
 * amendment, 2026-11-03), so there is one file.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    counts: LocalDataCounts?,
    saving: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("Export data", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(exportSummary(counts), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Text("Backup file (JSON)", style = MaterialTheme.typography.titleSmall)
            Text(
                "Can be imported back into GymBuddy, now and in future versions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onSave, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save file")
                }
            }
        }
    }
}

/** "84 workouts, 6 templates, your exercises, notes and settings for rest." */
internal fun exportSummary(counts: LocalDataCounts?): String {
    val head = if (counts == null || counts.workouts == 0 && counts.templates == 0) {
        "Your profile"
    } else {
        countsText(counts.copy(exercises = 0))
    }
    return "$head, your exercises, notes and settings for rest."
}
