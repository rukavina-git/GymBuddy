package com.rukavina.gymbuddy.data.sync

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.rukavina.gymbuddy.domain.sync.RecordingSyncRequester
import com.rukavina.gymbuddy.domain.sync.SyncReason
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * Scenario 11: sync triggers. App foreground and session completion are
 * the two the plan names; the network, coalescing, signed-out and
 * periodic paths are covered here too. Session completion's own test is
 * in CreateWorkoutSessionUseCaseTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncTriggersTest {

    // ---- app foreground ----

    @Test
    fun `app foreground requests a sync every time the process comes to the foreground`() = runTest {
        val requester = RecordingSyncRequester()
        val process = TestLifecycleOwner(Lifecycle.State.CREATED, UnconfinedTestDispatcher(testScheduler))
        SyncTriggers.observeForeground(process.lifecycle, requester)
        assertEquals(emptyList<SyncReason>(), requester.requested)

        process.currentState = Lifecycle.State.RESUMED
        assertEquals(listOf(SyncReason.APP_FOREGROUND), requester.requested)

        // Backgrounded, then foregrounded again.
        process.currentState = Lifecycle.State.CREATED
        process.currentState = Lifecycle.State.STARTED
        assertEquals(listOf(SyncReason.APP_FOREGROUND, SyncReason.APP_FOREGROUND), requester.requested)
    }

    // ---- scheduler: network, coalescing, signed out ----

    private class Harness(scope: CoroutineScope, var signedIn: Boolean = true, var outbox: Int = 0) {
        var cycles = 0
        var gate: CompletableDeferred<Unit>? = null
        val scheduler = SyncScheduler(
            runCycle = { cycles++; gate?.await() },
            outboxCount = { outbox },
            isSignedIn = { signedIn },
            scope = scope
        )
    }

    @Test
    fun `network available syncs only when the outbox has something to push`() = runTest {
        val harness = Harness(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        harness.scheduler.onNetworkAvailable()
        assertEquals("empty outbox: nothing to do", 0, harness.cycles)

        harness.outbox = 3
        harness.scheduler.onNetworkAvailable()
        assertEquals(1, harness.cycles)
    }

    @Test
    fun `requests during a running sync coalesce into one follow-up cycle`() = runTest {
        val harness = Harness(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        harness.gate = CompletableDeferred()

        harness.scheduler.requestSync(SyncReason.APP_FOREGROUND)
        repeat(5) { harness.scheduler.requestSync(SyncReason.SESSION_COMPLETED) }
        assertEquals(1, harness.cycles)

        harness.gate!!.complete(Unit)
        assertEquals("five queued requests become exactly one more cycle", 2, harness.cycles)
    }

    @Test
    fun `nothing syncs while signed out`() = runTest {
        val harness = Harness(CoroutineScope(UnconfinedTestDispatcher(testScheduler)), signedIn = false, outbox = 5)

        harness.scheduler.requestSync(SyncReason.APP_FOREGROUND)
        harness.scheduler.onNetworkAvailable()
        val ran = harness.scheduler.syncNow(SyncReason.MANUAL)

        assertEquals(0, harness.cycles)
        assertEquals(false, ran)
    }

    @Test
    fun `syncNow reports a failed cycle`() = runTest {
        val scheduler = SyncScheduler(
            runCycle = { throw SyncException("offline") },
            outboxCount = { 0 },
            isSignedIn = { true },
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        )
        assertEquals(false, scheduler.syncNow(SyncReason.MANUAL))
    }

    // ---- periodic ----

    @Test
    fun `periodic sync is scheduled every 6 hours, only on a network, and not duplicated`() {
        val context = RuntimeEnvironment.getApplication()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().build())
        val workManager = WorkManager.getInstance(context)

        SyncTriggers.schedulePeriodic(workManager)
        SyncTriggers.schedulePeriodic(workManager)

        val work = workManager.getWorkInfosForUniqueWork(SyncTriggers.PERIODIC_WORK_NAME).get()
        val info = work.single()
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        assertEquals(TimeUnit.HOURS.toMillis(6), info.periodicityInfo!!.repeatIntervalMillis)
        assertEquals(NetworkType.CONNECTED, info.constraints.requiredNetworkType)
    }

    private fun worker(requester: RecordingSyncRequester, signedIn: Boolean): SyncWorker {
        val auth = object : AuthSession {
            override val currentUid = if (signedIn) "uid" else null
            override suspend fun idToken() = null
            override fun signOut() = Unit
        }
        return TestListenableWorkerBuilder<SyncWorker>(RuntimeEnvironment.getApplication())
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
                    SyncWorker(appContext, workerParameters, requester, auth)
            })
            .build()
    }

    @Test
    fun `the periodic worker runs a PERIODIC sync and retries if it fails`() = runTest {
        val ok = RecordingSyncRequester(syncNowResult = true)
        assertEquals(ListenableWorker.Result.success(), worker(ok, signedIn = true).doWork())
        assertEquals(listOf(SyncReason.PERIODIC), ok.syncedNow)

        val failing = RecordingSyncRequester(syncNowResult = false)
        assertEquals(ListenableWorker.Result.retry(), worker(failing, signedIn = true).doWork())
    }

    @Test
    fun `the periodic worker does nothing while signed out`() = runTest {
        val requester = RecordingSyncRequester()
        assertEquals(ListenableWorker.Result.success(), worker(requester, signedIn = false).doWork())
        assertTrue(requester.syncedNow.isEmpty())
    }
}
