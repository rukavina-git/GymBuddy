package com.rukavina.gymbuddy.ui.workout.service

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide side of the running workout (Plan 7 06c). The ViewModel
 * pushes [WorkoutNotice]s; [WorkoutService] shows them and keeps the
 * process in the foreground, so the buzz below fires with the app in the
 * background or the screen locked. The buzz holds a partial wake lock
 * until it fires, or the CPU could sleep through the rest.
 */
@Singleton
class WorkoutServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
) : WorkoutServiceStarter, RestBuzzScheduler {

    private val _notice = MutableStateFlow<WorkoutNotice?>(null)
    val notice: StateFlow<WorkoutNotice?> = _notice.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var buzzJob: Job? = null
    private val wakeLock by lazy {
        context.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GymBuddy:restBuzz")
            .apply { setReferenceCounted(false) }
    }

    override fun update(notice: WorkoutNotice?) {
        _notice.value = notice
        if (notice != null && !WorkoutService.running) start()
    }

    private fun start() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, WorkoutService::class.java))
        } catch (e: IllegalStateException) {
            // Background start not allowed (API 31+): the next update from
            // the app in the foreground tries again.
        } catch (e: SecurityException) {
            // Missing foreground service permission; the workout still runs.
        }
    }

    @Synchronized
    override fun schedule(leftSeconds: Long) {
        cancel()
        if (leftSeconds <= 0) return
        val ms = leftSeconds * 1000
        wakeLock.acquire(ms + WAKE_MARGIN_MS)
        buzzJob = scope.launch {
            delay(ms)
            RestBuzz.buzz(context)
            release()
        }
    }

    @Synchronized
    override fun cancel() {
        buzzJob?.cancel()
        buzzJob = null
        release()
    }

    @Synchronized
    private fun release() {
        if (wakeLock.isHeld) wakeLock.release()
    }

    private companion object {
        /** The wake lock outlives the rest a little, then times out on its own. */
        const val WAKE_MARGIN_MS = 5_000L
    }
}
