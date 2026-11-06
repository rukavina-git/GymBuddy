package com.rukavina.gymbuddy.ui.workout.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import com.rukavina.gymbuddy.MainActivity
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.theme.ColorTokens
import com.rukavina.gymbuddy.ui.workout.active.setValueText

/** The notification's three lines, worked out from a [WorkoutNotice]. */
data class WorkoutNotificationText(val title: String, val text: String?, val subText: String?)

/**
 * Figma 53:3470: header "Push A · 41:05", title "Bench Press · set 4 of 4",
 * text "82.5 kg × 8". The header's clock is the system chronometer: the
 * rest while resting (subtext "Push A · rest"), else the workout clock.
 */
fun workoutNotificationText(res: Resources, notice: WorkoutNotice): WorkoutNotificationText {
    val workout = notice.workoutTitle.ifBlank { res.getString(R.string.workout_empty_title) }
    val sub = when {
        notice.paused -> res.getString(R.string.workout_notice_paused, workout)
        notice.resting -> res.getString(R.string.workout_notice_rest, workout)
        else -> workout
    }
    val set = notice.set
    return when {
        notice.exerciseName == null -> WorkoutNotificationText(workout, res.getString(R.string.workout_notice_no_exercises), sub)
        set == null || notice.trackingType == null ->
            WorkoutNotificationText(notice.exerciseName, res.getString(R.string.live_bar_all_done), sub)
        else -> WorkoutNotificationText(
            title = res.getString(R.string.workout_notice_set, notice.exerciseName, set.setNumber, notice.setCount),
            text = setValueText(res, notice.trackingType, set, notice.units),
            subText = sub,
        )
    }
}

/** Channel, notification and its tap target (Plan 7 06c). */
object WorkoutNotification {
    const val CHANNEL_ID = "workout"
    const val ID = 1006

    /** Low importance: no sound, no peek; shows in the shade and on the lock screen. */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.workout_notice_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun build(context: Context, notice: WorkoutNotice?): android.app.Notification {
        val open = openIntent(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_workout)
            .setColor(ColorTokens.Lime.toArgb())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(open)
            // No Done here (06 decision 2): it could lose a set if the app is dead.
            .addAction(0, context.getString(R.string.workout_notice_open), open)
        if (notice == null) {
            return builder.setContentTitle(context.getString(R.string.workout_empty_title)).build()
        }
        val text = workoutNotificationText(context.resources, notice)
        builder.setContentTitle(text.title).setContentText(text.text).setSubText(text.subText)
        val base = notice.counterBase
        if (base != null) {
            builder.setWhen(base).setShowWhen(true).setUsesChronometer(true)
        } else {
            builder.setShowWhen(false).setUsesChronometer(false)
        }
        return builder.build()
    }

    /** Like the launcher: brings the app's task back where the user left it. */
    private fun openIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
