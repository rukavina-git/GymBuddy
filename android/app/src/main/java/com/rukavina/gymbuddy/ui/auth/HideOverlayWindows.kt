package com.rukavina.gymbuddy.ui.auth

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import java.util.WeakHashMap

/**
 * While this is in composition, other apps' overlay windows are hidden,
 * so nothing can draw over a password field and steal taps (tapjacking,
 * MASTG-TEST-0340). Android 12+ (needs HIDE_OVERLAY_WINDOWS); older
 * phones do nothing. Only on password screens and dialogs, so screen
 * dimmers and chat heads keep working everywhere else.
 */
@Composable
internal fun HideOverlayWindows() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val context = LocalContext.current
    // Install-time permission, so always granted in the app; not in Robolectric.
    if (context.checkSelfPermission(Manifest.permission.HIDE_OVERLAY_WINDOWS) != PackageManager.PERMISSION_GRANTED) return
    val window = context.findActivity()?.window ?: return
    DisposableEffect(window) {
        windowOverlays.acquire(window)
        onDispose { windowOverlays.release(window) }
    }
}

private val windowOverlays = OverlayHideCounter<Window> { window, hide ->
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) window.setHideOverlayWindows(hide)
}

/**
 * Counts holders per window, so a screen with two password fields (or a
 * dialog over a sign-in screen) only lets overlays back when the last
 * one leaves. Main thread only.
 */
internal class OverlayHideCounter<K : Any>(private val apply: (K, Boolean) -> Unit) {
    private val counts = WeakHashMap<K, Int>()

    fun acquire(key: K) {
        val count = counts[key] ?: 0
        counts[key] = count + 1
        if (count == 0) apply(key, true)
    }

    fun release(key: K) {
        val count = counts[key] ?: return
        if (count <= 1) {
            counts.remove(key)
            apply(key, false)
        } else {
            counts[key] = count - 1
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
