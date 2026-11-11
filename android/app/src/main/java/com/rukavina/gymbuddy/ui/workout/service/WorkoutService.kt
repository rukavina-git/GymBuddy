package com.rukavina.gymbuddy.ui.workout.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service while a workout runs (Plan 7 06c): the ongoing
 * notification (Figma 53:3470, Open only) and a foreground process for
 * the rest buzz. `specialUse` type, subtype "workout timer" (manifest);
 * needs a Play Console justification at release.
 *
 * Follows [WorkoutServiceController.notice]: null stops it.
 */
@AndroidEntryPoint
class WorkoutService : Service() {

    @Inject lateinit var controller: WorkoutServiceController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watching: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        WorkoutNotification.createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        running = true
        // startForeground first, always: a started-for-foreground service
        // that stops without it crashes the app.
        ServiceCompat.startForeground(
            this,
            WorkoutNotification.ID,
            WorkoutNotification.build(this, controller.notice.value),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
        if (watching == null) {
            watching = scope.launch {
                controller.notice.collect { notice ->
                    if (notice == null) {
                        // A workout started before onDestroy starts it again.
                        running = false
                        ServiceCompat.stopForeground(this@WorkoutService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        getSystemService(NotificationManager::class.java)
                            .notify(WorkoutNotification.ID, WorkoutNotification.build(this@WorkoutService, notice))
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** Read by the controller so it starts the service only once. */
        @Volatile
        var running: Boolean = false
            private set
    }
}
