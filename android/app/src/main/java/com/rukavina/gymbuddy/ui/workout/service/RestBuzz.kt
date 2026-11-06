package com.rukavina.gymbuddy.ui.workout.service

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * The rest buzz (motion.md "Rest buzz"): 80 on · 120 off · 80 on, once.
 * Raw ms, not tokens (DESIGN.md §4). Follows the phone: no buzz in silent
 * or Do Not Disturb; `USAGE_NOTIFICATION` so the system applies its own
 * notification-vibration rules too (no alarm bypass).
 */
object RestBuzz {
    const val PULSE_MS = 80L
    const val GAP_MS = 120L
    val WAVEFORM = longArrayOf(0, PULSE_MS, GAP_MS, PULSE_MS)

    /** Sound and vibrate modes buzz; silent and any DND filter don't. */
    fun allowed(ringerMode: Int, interruptionFilter: Int): Boolean =
        ringerMode != AudioManager.RINGER_MODE_SILENT &&
            interruptionFilter <= NotificationManager.INTERRUPTION_FILTER_ALL

    fun buzz(context: Context) {
        val audio = context.getSystemService(AudioManager::class.java)
        val notifications = context.getSystemService(NotificationManager::class.java)
        if (!allowed(
                audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL,
                notifications?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL,
            )
        ) {
            return
        }
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        if (vibrator == null || !vibrator.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(WAVEFORM, -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_NOTIFICATION))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        }
    }
}
