package com.rukavina.gymbuddy.ui.settings.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.local.entity.OutboxEntryEntity
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.local.entity.SyncLogEntity
import com.rukavina.gymbuddy.data.sync.SyncLocalStore
import com.rukavina.gymbuddy.data.sync.SyncScheduler
import com.rukavina.gymbuddy.data.sync.SyncStatus
import com.rukavina.gymbuddy.domain.sync.SyncOutcome
import com.rukavina.gymbuddy.domain.sync.SyncReason
import com.rukavina.gymbuddy.domain.sync.SyncRequester
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncDebugUiState(
    /** Pending changes per entity type, only types with at least one. */
    val outboxCounts: Map<SyncEntityType, Int> = emptyMap(),
    val outbox: List<OutboxEntryEntity> = emptyList(),
    /** The pull cursor, shortened. Null before the first pull. */
    val cursor: String? = null,
    val status: SyncStatus = SyncStatus(),
    /** Discarded-change log, newest first. */
    val discarded: List<SyncLogEntity> = emptyList(),
    val isSyncing: Boolean = false,
    /** Set when "Sync now" was skipped because nobody is signed in. */
    val signedOut: Boolean = false
) {
    val isEmpty: Boolean get() = outbox.isEmpty() && discarded.isEmpty() && cursor == null && status.lastSyncAt == null
}

/** Debug builds only: live sync state from Room and the scheduler. Read-only apart from "Sync now". */
@HiltViewModel
class SyncDebugViewModel @Inject constructor(
    store: SyncLocalStore,
    scheduler: SyncScheduler,
    private val requester: SyncRequester
) : ViewModel() {

    private val manual = MutableStateFlow(ManualSync())

    val uiState: StateFlow<SyncDebugUiState> = combine(
        store.observeOutbox(),
        store.observeCursor(),
        store.observeLog(),
        scheduler.status,
        manual
    ) { outbox, cursor, log, status, manual ->
        SyncDebugUiState(
            outboxCounts = outbox.groupingBy { it.entityType }.eachCount().toSortedMap(),
            outbox = outbox,
            cursor = cursor?.let(::shorten),
            status = status,
            discarded = log,
            isSyncing = manual.running,
            signedOut = manual.signedOut
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncDebugUiState())

    /** Runs a cycle through the same requester the sync triggers use, never the engine directly. */
    fun syncNow() {
        if (manual.value.running) return
        manual.value = ManualSync(running = true)
        viewModelScope.launch {
            val outcome = requester.syncNow(SyncReason.MANUAL)
            manual.value = ManualSync(signedOut = outcome == SyncOutcome.SIGNED_OUT)
        }
    }

    private data class ManualSync(val running: Boolean = false, val signedOut: Boolean = false)

    companion object {
        private const val KEEP = 8

        /** The cursor is opaque and long; its ends are enough to tell two apart. */
        fun shorten(cursor: String): String =
            if (cursor.length <= KEEP * 2 + 1) cursor else "${cursor.take(KEEP)}…${cursor.takeLast(KEEP)}"
    }
}
