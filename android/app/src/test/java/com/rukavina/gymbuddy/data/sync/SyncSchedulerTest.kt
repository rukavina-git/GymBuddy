package com.rukavina.gymbuddy.data.sync

import com.rukavina.gymbuddy.domain.sync.SyncOutcome
import com.rukavina.gymbuddy.domain.sync.SyncReason
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * SyncScheduler must never drop a request, including one that arrives
 * just as a cycle finishes (after the loop's last rerun check, before
 * running is cleared). Robolectric only because the scheduler logs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncSchedulerTest {

    @Test
    fun `a request made while a cycle runs causes exactly one extra cycle`() = runTest {
        var cycles = 0
        val gate = CompletableDeferred<Unit>()
        val scheduler = SyncScheduler(
            runCycle = { if (++cycles == 1) gate.await() },
            outboxCount = { 0 },
            isSignedIn = { true },
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        )

        scheduler.requestSync(SyncReason.APP_FOREGROUND)
        scheduler.requestSync(SyncReason.SESSION_COMPLETED)
        assertEquals(1, cycles)

        gate.complete(Unit)
        assertEquals(2, cycles)
    }

    @Test
    fun `a request racing the end of a cycle is never dropped`() = runBlocking {
        // Each trial: a second thread spins until the first cycle returns,
        // then requests at once, aiming at the gap between the loop's last
        // rerun check and running being cleared. Whichever side of the gap
        // it lands on, exactly one more cycle must run.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            repeat(2_000) { trial ->
                val cycles = AtomicInteger(0)
                val firstCycleDone = AtomicBoolean(false)
                val scheduler = SyncScheduler(
                    runCycle = { if (cycles.incrementAndGet() == 1) firstCycleDone.set(true) },
                    outboxCount = { 0 },
                    isSignedIn = { true },
                    scope = scope
                )
                val racer = thread {
                    while (!firstCycleDone.get()) Thread.onSpinWait()
                    scheduler.requestSync(SyncReason.SESSION_COMPLETED)
                }

                scheduler.requestSync(SyncReason.APP_FOREGROUND)
                racer.join()
                val served = withTimeoutOrNull(1_000) {
                    while (cycles.get() < 2) delay(1)
                }
                assertNotNull("trial $trial: the racing request was dropped", served)
                delay(1)
                assertEquals("trial $trial", 2, cycles.get())
            }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `signed out, no cycle runs`() = runTest {
        var cycles = 0
        val scheduler = SyncScheduler(
            runCycle = { cycles++ },
            outboxCount = { 3 },
            isSignedIn = { false },
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        )

        repeat(10) { scheduler.requestSync(SyncReason.SESSION_COMPLETED) }
        scheduler.onNetworkAvailable()

        assertEquals(0, cycles)
    }

    @Test
    fun `status records the last background cycle, success after failure`() = runTest {
        var fail = true
        var clock = 10L
        val scheduler = SyncScheduler(
            runCycle = { if (fail) throw SyncException("push failed: HTTP 503", 503, traceId = "t-1") },
            outboxCount = { 0 },
            isSignedIn = { true },
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            now = { clock }
        )

        scheduler.requestSync(SyncReason.APP_FOREGROUND)
        assertEquals(SyncStatus(10L, SyncOutcome.FAILED, "push failed: HTTP 503", "t-1"), scheduler.status.value)

        fail = false
        clock = 20L
        scheduler.requestSync(SyncReason.PERIODIC)
        assertEquals(SyncStatus(20L, SyncOutcome.SYNCED, null, null), scheduler.status.value)
    }
}
