package com.rukavina.gymbuddy.ui.settings

import com.rukavina.gymbuddy.data.local.entity.SyncEntityType

/** Where logout stands, for SettingsScreen. */
sealed class LogoutUiState {
    data object Idle : LogoutUiState()
    /** Pushing unsynced changes before signing out. */
    data object Flushing : LogoutUiState()
    /** The push failed: warn before discarding these, grouped by entity type. */
    data class UnsyncedWarning(val lines: List<String>) : LogoutUiState()
}

/** One human-readable line per entity type with unsynced changes, e.g. "2 workout sessions". */
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
        }
        "$count ${if (count == 1) singular else plural}"
    }
