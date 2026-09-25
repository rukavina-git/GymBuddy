package com.rukavina.gymbuddy.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rukavina.gymbuddy.domain.sync.SyncReason
import com.rukavina.gymbuddy.domain.sync.SyncRequester
import java.util.concurrent.TimeUnit

/**
 * The automatic sync triggers:
 * - app foreground: [observeForeground] on the process lifecycle
 * - connectivity returns with a non-empty outbox: [observeNetwork]
 * - periodically, about every 6h: [schedulePeriodic]
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
}
