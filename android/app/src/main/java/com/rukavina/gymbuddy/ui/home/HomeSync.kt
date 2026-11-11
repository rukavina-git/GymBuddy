package com.rukavina.gymbuddy.ui.home

import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.sync.Resync
import com.rukavina.gymbuddy.data.sync.SyncEngine
import com.rukavina.gymbuddy.data.sync.SyncLocalStore
import com.rukavina.gymbuddy.data.sync.SyncScheduler
import com.rukavina.gymbuddy.domain.sync.SyncReason
import com.rukavina.gymbuddy.ui.you.OnlineCheck
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** What Home reads about backup and re-download (R17-C1b, C2a). A seam for tests. */
interface HomeSyncSignals {
    /** When the oldest unsent change was made; null if nothing waits (or a guest, who never backs up). */
    val oldestWaiting: Flow<Long?>
    val resync: Flow<Resync>
    fun isOnline(): Boolean
    /** One sync cycle, now; returns when it's done (either way). */
    suspend fun backUpNow()
    /** R2 Retry: one more cycle in the background. */
    fun retryResync()
}

class RoomHomeSyncSignals @Inject constructor(
    store: SyncLocalStore,
    preferences: AppPreferencesRepository,
    engine: SyncEngine,
    private val scheduler: SyncScheduler,
    private val onlineCheck: OnlineCheck,
) : HomeSyncSignals {
    override val oldestWaiting: Flow<Long?> = combine(
        store.observeOutbox().catch { emit(emptyList()) },
        preferences.isGuest,
    ) { outbox, guest -> if (guest) null else outbox.minOfOrNull { it.enqueuedAt } }

    override val resync: Flow<Resync> = engine.resync

    override fun isOnline(): Boolean = onlineCheck.isOnline()

    override suspend fun backUpNow() {
        scheduler.syncNow(SyncReason.MANUAL)
    }

    override fun retryResync() = scheduler.requestSync(SyncReason.MANUAL)
}

/** Home's line in the week card (R17-C1b, R1, E3). */
sealed interface HomeBackup {
    data object Hidden : HomeBackup
    /** "Not backed up for N days · Retry". */
    data class Stuck(val days: Int) : HomeBackup
    /** R1: Retry tapped, the cycle is running. */
    data object BackingUp : HomeBackup
    /** E3: Retry while offline, for [OFFLINE_NOTICE_MS], then back to Stuck. */
    data object Offline : HomeBackup
}

/** Home waits longer than You (24 h) before it says anything (step 11 decision 5). */
internal const val HOME_BACKUP_AFTER_DAYS = 3
internal const val OFFLINE_NOTICE_MS = 4_000L
private const val DAY_MS = 24 * 60 * 60 * 1000L

/** Whole days the oldest change has waited, or null under [HOME_BACKUP_AFTER_DAYS]. */
internal fun stuckDays(oldestWaiting: Long?, now: Long): Int? =
    oldestWaiting?.let { ((now - it) / DAY_MS).toInt() }?.takeIf { it >= HOME_BACKUP_AFTER_DAYS }
