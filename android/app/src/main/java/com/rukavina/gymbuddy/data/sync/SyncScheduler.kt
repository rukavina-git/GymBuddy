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
        // Flag first, then try to claim. If the flag were set after a failed
        // claim, the runner could finish its last check and exit in between.
        rerunRequested.set(true)
        if (!running.compareAndSet(false, true)) return
        scope.launch {
            // A request can still land after the inner loop's last check but
            // before running is cleared: its claim fails and it only leaves the
            // flag. Check again once running is cleared, so it isn't lost.
            do {
                try {
                    do {
                        rerunRequested.set(false)
                        runCatching { runCycle() }.onFailure { Log.w(TAG, "sync ($reason) failed", it) }
                    } while (rerunRequested.get())
                } finally {
                    running.set(false)
                }
            } while (rerunRequested.get() && isSignedIn() && running.compareAndSet(false, true))
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
