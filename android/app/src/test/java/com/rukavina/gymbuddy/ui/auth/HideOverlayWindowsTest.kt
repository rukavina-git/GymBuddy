package com.rukavina.gymbuddy.ui.auth

import android.Manifest
import android.app.Activity
import android.app.Application
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class HideOverlayWindowsTest {
    @get:Rule val rule = createComposeRule()

    // Hidden framework field and flag, real in Robolectric's android-all.
    private val privateFlags = WindowManager.LayoutParams::class.java.getField("privateFlags")
    private val hideFlag = WindowManager.LayoutParams::class.java
        .getField("SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS").getInt(null)

    private fun Activity.hidesOverlays() = privateFlags.getInt(window.attributes) and hideFlag != 0

    @Test
    fun `hides overlays while shown, shows them again after`() {
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .grantPermissions(Manifest.permission.HIDE_OVERLAY_WINDOWS)
        var show by mutableStateOf(true)
        lateinit var activity: Activity
        rule.setContent {
            activity = LocalActivity.current!!
            if (show) HideOverlayWindows()
        }
        rule.waitForIdle()
        assertTrue(activity.hidesOverlays())

        show = false
        rule.waitForIdle()
        assertFalse(activity.hidesOverlays())
    }

    @Test
    fun `does nothing without the permission`() {
        lateinit var activity: Activity
        rule.setContent {
            activity = LocalActivity.current!!
            HideOverlayWindows()
        }
        rule.waitForIdle()
        assertFalse(activity.hidesOverlays())
    }
}
