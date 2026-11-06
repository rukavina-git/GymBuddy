package com.rukavina.gymbuddy.ui.you

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** "Is there a validated network right now?" for the sign-out dialog (R15-13). Open for tests. */
open class OnlineCheck @Inject constructor(@ApplicationContext private val context: Context) {
    open fun isOnline(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
