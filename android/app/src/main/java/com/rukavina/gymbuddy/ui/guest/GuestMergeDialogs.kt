package com.rukavina.gymbuddy.ui.guest

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.rukavina.gymbuddy.data.sync.GuestMergeState

/**
 * Plain dialogs for the guest → account merge, worded as Figma
 * "13 · Guest data into an account with data" and "14 · Receipt"
 * (node 114-20520). Restyled in Plan 7.4.
 */
@Composable
fun GuestMergeDialogs(
    state: GuestMergeState,
    onAdd: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    when (state) {
        is GuestMergeState.Asking -> AlertDialog(
            onDismissRequest = {}, // a choice is needed: Add or Cancel
            title = { Text("Add this phone's workouts?") },
            text = { Text(askText(state)) },
            confirmButton = { Button(onClick = onAdd) { Text("Add and sign in") } },
            dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
        )
        is GuestMergeState.Done -> AlertDialog(
            onDismissRequest = onDismiss,
            text = { Text(receiptText(state.receipt)) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
        )
        GuestMergeState.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            text = { Text("Couldn't add this phone's workouts. Nothing was lost; we'll ask again next time.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
        )
        GuestMergeState.Idle, GuestMergeState.Merging -> Unit
    }
}
