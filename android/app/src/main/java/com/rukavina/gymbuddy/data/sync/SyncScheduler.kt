package com.rukavina.gymbuddy.data.sync

import android.util.Log
import com.rukavina.gymbuddy.data.crash.CrashReporter
import com.rukavina.gymbuddy.data.crash.NoOpCrashReporter
import com.rukavina.gymbuddy.domain.sync.SyncOutcome
import com.rukavina.gymbuddy.domain.sync.SyncReason
import com.rukavina.gymbuddy.domain.sync.SyncRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

/**
 * How the last sync cycle went, for the debug screen. In memory only:
 * lost on process death, which is fine for a debug tool.
 */
data class SyncStatus(
    val lastSyncAt: Long? = null,
    val lastOutcome: SyncOutcome? = null,
    val lastError: String? = null,
    val lastTraceId: String? = null
)

/**
 * Turns sync requests into sync cycles. Requests that arrive while a
 * cycle is running are coalesced into one follow-up cycle, not queued
 * one per request. Nothing runs while signed out.
 *
 * Takes plain functions rather than the engine so the trigger logic can
 * be tested without a network. SyncModule wires in the real
 * SyncEngine.sync and outbox count.
 *
 * A failed cycle is reported as a non-fatal, except when it was
 * cancelled or the device was offline: those are expected, not bugs.
 */
class SyncScheduler(
    private val runCycle: suspend () -> Unit,
    private val outboxCount: suspend () -> Int,
    private val isSignedIn: () -> Boolean,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
    private val crashReporter: CrashReporter = NoOpCrashReporter
) : SyncRequester {

    private val running = AtomicBoolean(false)
    private val rerunRequested = AtomicBoolean(false)

    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

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
                    // Consume the flag before each cycle. A late claimer whose
                    // request an earlier cycle already served finds it cleared
                    // and runs nothing.
                    while (rerunRequested.getAndSet(false)) runAndRecord(reason)
                } finally {
                    running.set(false)
                }
            } while (rerunRequested.get() && isSignedIn() && running.compareAndSet(false, true))
        }
    }

    override suspend fun syncNow(reason: SyncReason): SyncOutcome {
        if (!isSignedIn()) return SyncOutcome.SIGNED_OUT
        Log.d(TAG, "sync now: $reason")
        return runAndRecord(reason)
    }

    private suspend fun runAndRecord(reason: SyncReason): SyncOutcome {
        val result = runCatching { runCycle() }.onFailure { Log.w(TAG, "sync ($reason) failed", it) }
        val error = result.exceptionOrNull()
        val outcome = if (error == null) SyncOutcome.SYNCED else SyncOutcome.FAILED
        if (error != null && isReportable(error)) report(error, reason)
        _status.value = SyncStatus(
            lastSyncAt = now(),
            lastOutcome = outcome,
            lastError = error?.let { it.message ?: it::class.simpleName },
            lastTraceId = (error as? SyncException)?.traceId
        )
        return outcome
    }

    private fun isReportable(error: Throwable) =
        error !is CancellationException && generateSequence(error) { it.cause }.none { it is IOException }

    private fun report(error: Throwable, reason: SyncReason) {
        val sync = error as? SyncException
        val keys = buildMap {
            put("reason", reason.name)
            sync?.traceId?.let { put("traceId", it) }
            sync?.httpStatus?.let { put("httpStatus", it.toString()) }
        }
        crashReporter.recordNonFatal(error, keys)
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
