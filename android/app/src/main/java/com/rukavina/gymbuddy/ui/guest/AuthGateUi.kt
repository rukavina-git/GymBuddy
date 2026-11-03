package com.rukavina.gymbuddy.ui.guest

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.data.sync.LocalDataCounts

/*
 * Plain session-expired banner and account-deleted dialog, worded as Figma
 * "16 · Session expired" and "20 · Account deleted elsewhere" (node
 * 114-20520). Restyled in Plan 7.4.
 */

@Composable
fun SessionExpiredBanner(onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Backup paused. Sign in again to keep backing up.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onSignIn) { Text("Sign in") }
        }
    }
}

@Composable
fun AccountDeletedDialog(
    email: String?,
    counts: LocalDataCounts?,
    onKeep: () -> Unit,
    onErase: () -> Unit
) {
    var confirmErase by remember { mutableStateOf(false) }
    val what = counts?.let { if (it.isEmpty) "Your data is" else "Your ${countsText(it)} are" } ?: "Your data is"
    val who = email?.let(::maskEmail) ?: "Your account"
    if (!confirmErase) {
        AlertDialog(
            onDismissRequest = {}, // a choice is needed
            text = { Text("$who was deleted. $what still on this phone.") },
            confirmButton = { Button(onClick = onKeep) { Text("Keep on this phone") } },
            dismissButton = { TextButton(onClick = { confirmErase = true }) { Text("Erase…") } }
        )
    } else {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text(counts?.takeUnless { it.isEmpty }?.let { "Erase ${countsText(it)}?" } ?: "Erase all data?") },
            text = { Text("There's no other copy. This can't be undone.") },
            confirmButton = {
                Button(
                    onClick = onErase,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Erase") }
            },
            dismissButton = { TextButton(onClick = { confirmErase = false }) { Text("Cancel") } }
        )
    }
}
