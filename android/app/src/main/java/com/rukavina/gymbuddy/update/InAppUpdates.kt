package com.rukavina.gymbuddy.update

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Play in-app updates (0.2.2+). Checked on every resume; Play's own
 * full-screen update UI does the rest. A forced update the user backs
 * out of closes the app; a prompt shows once per version.
 *
 * Off-Play installs (qa via App Distribution, debug) get an error from
 * Play and nothing happens.
 */
class InAppUpdates(
    private val activity: Activity,
    private val manager: AppUpdateManager,
    private val launcher: ActivityResultLauncher<IntentSenderRequest>,
    /** Every check's outcome; NONE also when Play can't answer. */
    private val onDecided: (UpdateAction) -> Unit = {},
) {
    private val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var forced = false

    fun check() {
        manager.appUpdateInfo
            .addOnSuccessListener(::act)
            .addOnFailureListener {
                Log.d(TAG, "update check unavailable: ${it.message}")
                onDecided(UpdateAction.NONE)
            }
    }

    /** Result of Play's update screen. Backing out of a forced update closes the app. */
    fun onResult(resultCode: Int) {
        if (forced && resultCode != Activity.RESULT_OK) activity.finish()
    }

    private fun act(info: AppUpdateInfo) {
        val offer = UpdateOffer(
            available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE,
            inProgress = info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
            priority = info.updatePriority(),
            versionCode = info.availableVersionCode(),
            immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
        )
        val lastPrompted = prefs.getInt(KEY_PROMPTED, NONE).takeIf { it != NONE }
        val action = UpdatePolicy.decide(offer, lastPrompted)
        onDecided(action)
        when (action) {
            UpdateAction.NONE -> return
            UpdateAction.FORCE -> forced = true
            UpdateAction.PROMPT -> {
                forced = false
                prefs.edit { putInt(KEY_PROMPTED, offer.versionCode) }
            }
        }
        manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE))
    }

    private companion object {
        const val TAG = "InAppUpdates"
        const val PREFS = "in_app_updates"
        const val KEY_PROMPTED = "prompted_version_code"
        const val NONE = -1
    }
}
