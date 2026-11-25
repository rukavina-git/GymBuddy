package com.rukavina.gymbuddy.update

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.rukavina.gymbuddy.BuildConfig
import com.rukavina.gymbuddy.MainActivity

/**
 * Play's update manager, or in debug builds a fake one when the app is
 * started with [EXTRA_FAKE_PRIORITY], to check the update transitions
 * on a USB phone:
 * adb shell am start -n <pkg>/com.rukavina.gymbuddy.MainActivity --ei debug_fake_update_priority 4
 */
internal object UpdateManagers {
    const val EXTRA_FAKE_PRIORITY = "debug_fake_update_priority"
    /** In src/debug, so named, not referenced: <root package>.debug.FakePlayUpdateActivity. */
    private val FAKE_SCREEN = MainActivity::class.java.name.substringBeforeLast('.') + ".debug.FakePlayUpdateActivity"

    fun create(activity: Activity, debug: Boolean = BuildConfig.DEBUG): AppUpdateManager {
        val priority = activity.intent?.getIntExtra(EXTRA_FAKE_PRIORITY, -1) ?: -1
        if (!debug || priority < 0) return AppUpdateManagerFactory.create(activity)
        return ScreenFakeUpdateManager(activity).apply {
            setUpdateAvailable(Int.MAX_VALUE, AppUpdateType.IMMEDIATE)
            setUpdatePriority(priority)
        }
    }

    /** Play's fake, but its update flow opens a real (debug) screen, like Play does. */
    internal class ScreenFakeUpdateManager(private val context: Context) : FakeAppUpdateManager(context) {
        override fun startUpdateFlowForResult(
            info: AppUpdateInfo,
            launcher: ActivityResultLauncher<IntentSenderRequest>,
            options: AppUpdateOptions,
        ): Boolean {
            val intent = Intent().setComponent(ComponentName(context.packageName, FAKE_SCREEN))
            val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            launcher.launch(IntentSenderRequest.Builder(pending).build())
            return true
        }
    }
}
