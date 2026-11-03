package com.rukavina.gymbuddy.ui.guest

import com.rukavina.gymbuddy.data.sync.GuestMergeReceipt
import com.rukavina.gymbuddy.data.sync.GuestMergeState
import com.rukavina.gymbuddy.data.sync.LocalDataCounts

// Plain text for the guest and auth-gate dialogs, kept apart from the
// composables so it can be unit-tested (GuestTextTest).

internal fun askText(state: GuestMergeState.Asking): String {
    val account = state.accountEmail ?: "Your account"
    val has = if (state.accountWorkouts > 0) {
        "$account already has ${plural(state.accountWorkouts, "workout")}. "
    } else {
        "We couldn't check what $account already has. "
    }
    return "${has}Add the ${countsText(state.guest)} from this phone to it? Nothing is deleted."
}

internal fun receiptText(receipt: GuestMergeReceipt): String {
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

/** "k…@gmail.com", as in the Figma frames. */
fun maskEmail(email: String): String {
    val at = email.indexOf('@')
    return if (at <= 1) email else email.first() + "…" + email.substring(at)
}
