package com.rukavina.gymbuddy.data.sync

import android.util.Log
import com.rukavina.gymbuddy.domain.sync.SyncReason
import com.rukavina.gymbuddy.domain.sync.SyncRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Turns sync requests into sync cycles. Requests that arrive while a
 * cycle is running are coalesced into one follow-up cycle, not queued
 * one per request. Nothing runs while signed out.
 *
 * Takes plain functions rather than the engine so the trigger logic can
 * be tested without a network. SyncModule wires in the real
 * SyncEngine.sync and outbox count.
 */
class SyncScheduler(
    private val runCycle: suspend () -> Unit,
    private val outboxCount: suspend () -> Int,
    private val isSignedIn: () -> Boolean,
    private val scope: CoroutineScope
) : SyncRequester {

    private val running = AtomicBoolean(false)
    private val rerunRequested = AtomicBoolean(false)

    override fun requestSync(reason: SyncReason) {
        if (!isSignedIn()) return
        Log.d(TAG, "sync requested: $reason")
        if (!running.compareAndSet(false, true)) {
            rerunRequested.set(true)
            return
        }
        scope.launch {
            try {
                do {
                    rerunRequested.set(false)
                    runCatching { runCycle() }.onFailure { Log.w(TAG, "sync ($reason) failed", it) }
                } while (rerunRequested.get())
            } finally {
                running.set(false)
            }
        }
    }

    override suspend fun syncNow(reason: SyncReason): Boolean {
        if (!isSignedIn()) return false
        Log.d(TAG, "sync now: $reason")
        return runCatching { runCycle() }
            .onFailure { Log.w(TAG, "sync ($reason) failed", it) }
            .isSuccess
    }

    /** Connectivity came back: sync only if there's something waiting to be pushed. */
    fun onNetworkAvailable() {
        if (!isSignedIn()) return
        scope.launch {
            if (outboxCount() > 0) requestSync(SyncReason.NETWORK_AVAILABLE)
        }
    }

    private companion object {
        const val TAG = "SyncScheduler"
    }
}
