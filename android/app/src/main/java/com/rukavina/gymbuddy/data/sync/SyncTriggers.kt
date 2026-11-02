package com.rukavina.gymbuddy.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rukavina.gymbuddy.domain.sync.SyncReason
import com.rukavina.gymbuddy.domain.sync.SyncRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.TimeUnit

/**
 * The automatic sync triggers:
 * - app foreground: [observeForeground] on the process lifecycle
 * - connectivity returns with a non-empty outbox: [observeNetwork]
 * - periodically, about every 6h: [schedulePeriodic]
 * - the outbox goes non-empty: [observeOutbox] enqueues a one-off push
 *   that survives the app being swiped away
 *
 * The other triggers live with their callers. Session completion is in
 * CreateWorkoutSessionUseCase, pull-to-refresh in
 * WorkoutSessionViewModel, the pre-logout flush in LogoutCoordinator,
 * and sign-in in FirebaseAuthSession. None of them fire on an ordinary
 * local write.
 */
object SyncTriggers {

    const val PERIODIC_WORK_NAME = "sync-periodic"
    const val PERIODIC_INTERVAL_HOURS = 6L
    const val PUSH_OUTBOX_WORK_NAME = "push-outbox"
    const val PUSH_OUTBOX_BACKOFF_SECONDS = 30L

    fun observeForeground(processLifecycle: Lifecycle, requester: SyncRequester) {
        processLifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = requester.requestSync(SyncReason.APP_FOREGROUND)
        })
    }

    fun observeNetwork(context: Context, scheduler: SyncScheduler) {
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return
        connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = scheduler.onNetworkAvailable()
        })
    }

    fun schedulePeriodic(workManager: WorkManager) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /**
     * Each time the outbox goes from empty to non-empty (and once at start
     * if it already is), enqueue [PushOutboxWorker]. KEEP means a push
     * already waiting isn't duplicated. The network callback stays for the
     * app-open case; SyncScheduler's lock stops double runs.
     */
    fun observeOutbox(outboxCount: Flow<Int>, workManager: WorkManager, scope: CoroutineScope) {
        outboxCount
            .map { it > 0 }
            .distinctUntilChanged()
            .filter { it }
            .onEach { enqueuePush(workManager) }
            .launchIn(scope)
    }

    fun enqueuePush(workManager: WorkManager) {
        val request = OneTimeWorkRequestBuilder<PushOutboxWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, PUSH_OUTBOX_BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(PUSH_OUTBOX_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }
}
