package com.rukavina.gymbuddy.ui.guest

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.rukavina.gymbuddy.data.sync.GuestMergeReceipt
import com.rukavina.gymbuddy.data.sync.GuestMergeState
import com.rukavina.gymbuddy.data.sync.LocalDataCounts

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

private fun askText(state: GuestMergeState.Asking): String {
    val account = state.accountEmail ?: "Your account"
    val has = if (state.accountWorkouts > 0) {
        "$account already has ${plural(state.accountWorkouts, "workout")}. "
    } else {
        "We couldn't check what $account already has. "
    }
    return "${has}Add the ${countsText(state.guest)} from this phone to it? Nothing is deleted."
}

private fun receiptText(receipt: GuestMergeReceipt): String {
    val added = if (receipt.added.isEmpty) "Added your changes from this phone."
    else "Added ${countsText(receipt.added)} from this phone."
    return if (receipt.keptAccountProfile) "$added Kept your account's profile." else added
}

/** "5 workouts, 1 template and 2 exercises"; zero counts are left out. */
fun countsText(counts: LocalDataCounts): String {
    val parts = listOfNotNull(
        counts.workouts.takeIf { it > 0 }?.let { plural(it, "workout") },
        counts.templates.takeIf { it > 0 }?.let { plural(it, "template") },
        counts.exercises.takeIf { it > 0 }?.let { plural(it, "exercise") }
    )
    return when (parts.size) {
        0 -> "changes"
        1 -> parts[0]
        else -> parts.dropLast(1).joinToString(", ") + " and " + parts.last()
    }
}

private fun plural(n: Int, word: String) = "$n $word" + if (n == 1) "" else "s"
