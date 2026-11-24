package com.rukavina.gymbuddy.data.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller

/**
 * Updates the Play services security provider, which carries TLS fixes
 * newer than the phone's Android version (MASTG-TEST-0295). Blocking:
 * call it off the main thread, before the app's own network calls.
 * Without Play services (or in Robolectric) it logs and the platform
 * provider stays in use.
 */
internal object SecurityProvider {
    private const val TAG = "SecurityProvider"

    fun install(context: Context) {
        try {
            ProviderInstaller.installIfNeeded(context)
        } catch (e: GooglePlayServicesRepairableException) {
            Log.w(TAG, "Play services needs an update; using the platform provider", e)
        } catch (e: GooglePlayServicesNotAvailableException) {
            Log.w(TAG, "Play services missing; using the platform provider", e)
        }
    }
}
