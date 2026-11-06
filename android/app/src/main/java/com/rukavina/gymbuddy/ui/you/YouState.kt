package com.rukavina.gymbuddy.ui.you

import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.domain.model.PreferredUnits

data class YouUiState(
    val userName: String = "",
    val userEmail: String = "",
    val avatarId: String? = null,
    val isLoading: Boolean = true,
    val hasPassword: Boolean = false,
    val isGoogleOnly: Boolean = false,
    /** No account (ADR 0014): no sign out, password or delete account; erase instead. */
    val isGuest: Boolean = false,
    val units: PreferredUnits = PreferredUnits.METRIC,
    val restBuzz: Boolean = false,
    val hiddenExercises: Int = 0,
    val hiddenTemplates: Int = 0,
    val backup: BackupLine = BackupLine.BackedUp(null),
)

/** The account card's backup line (R15-2, R15-12). */
sealed interface BackupLine {
    /** Nothing waiting. [at] = last sync this process saw, null if none yet. */
    data class BackedUp(val at: Long?) : BackupLine
    data class Waiting(val changes: Int) : BackupLine
    /** The oldest change has waited over a day: "Couldn't back up · Retry". */
    data class Failed(val changes: Int) : BackupLine
}

internal const val BACKUP_FAILED_AFTER_MS = 24 * 60 * 60 * 1000L

/** [oldestEnqueuedAt] null = outbox empty. */
internal fun backupLine(changes: Int, oldestEnqueuedAt: Long?, lastSyncAt: Long?, now: Long): BackupLine = when {
    changes == 0 || oldestEnqueuedAt == null -> BackupLine.BackedUp(lastSyncAt)
    now - oldestEnqueuedAt > BACKUP_FAILED_AFTER_MS -> BackupLine.Failed(changes)
    else -> BackupLine.Waiting(changes)
}

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data object Saving : ExportUiState
    data object Done : ExportUiState
    data object Failed : ExportUiState
}

/** Where sign out stands (R15-8, R15-13). */
sealed class LogoutUiState {
    data object Idle : LogoutUiState()
    /** Changes are only on this phone: back up first or sign out anyway. [offline] adds the "connect first" line. */
    data class Confirm(val lines: List<String>, val offline: Boolean) : LogoutUiState()
    /** Pushing unsynced changes before signing out. */
    data object Flushing : LogoutUiState()
    /** The push failed: same question, these are still only here. */
    data class UnsyncedWarning(val lines: List<String>) : LogoutUiState()
    /** Logout threw. [afterWarning] says which step failed, so retry repeats it. */
    data class Failed(val message: String, val afterWarning: Boolean) : LogoutUiState()
}

/** Where account deletion stands. Done is the accountDeletedEvent. */
sealed class AccountDeletionUiState {
    data object Idle : AccountDeletionUiState()
    /** Deleting on the server, then wiping the device. */
    data object InProgress : AccountDeletionUiState()
    /** [canRetry] is false when the user must re-authenticate first (close, then start over). */
    data class Failed(val message: String, val canRetry: Boolean) : AccountDeletionUiState()
}

/** One line per entity type with unsynced changes, e.g. "2 workouts". */
fun unsyncedChangeLines(pendingByType: Map<SyncEntityType, Int>): List<String> =
    SyncEntityType.entries.mapNotNull { type ->
        val count = pendingByType[type] ?: 0
        if (count == 0) return@mapNotNull null
        val (singular, plural) = when (type) {
            SyncEntityType.WORKOUT_SESSION -> "workout session" to "workout sessions"
            SyncEntityType.EXERCISE -> "custom exercise" to "custom exercises"
            SyncEntityType.WORKOUT_TEMPLATE -> "workout template" to "workout templates"
            SyncEntityType.USER_EXERCISE_STATE -> "exercise preference" to "exercise preferences"
            SyncEntityType.USER_TEMPLATE_STATE -> "template preference" to "template preferences"
            SyncEntityType.USER_PROFILE -> "profile change" to "profile changes"
            SyncEntityType.BODY_WEIGHT_ENTRY -> "body weight entry" to "body weight entries"
        }
        "$count ${if (count == 1) singular else plural}"
    }
