package com.rukavina.gymbuddy.update

import android.app.Application
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import android.os.Looper
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import org.robolectric.Shadows.shadowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class UpdateManagersTest {
    private fun activity(priority: Int?) = Robolectric.buildActivity(
        ComponentActivity::class.java,
        Intent().apply { if (priority != null) putExtra(UpdateManagers.EXTRA_FAKE_PRIORITY, priority) },
    ).setup().get()

    private fun info(manager: AppUpdateManager): AppUpdateInfo {
        val task = manager.appUpdateInfo
        shadowOf(Looper.getMainLooper()).idle()
        return task.result
    }

    @Test
    fun `debug build with the extra gets the fake, with that priority`() {
        val manager = UpdateManagers.create(activity(4), debug = true)
        assertTrue(manager is UpdateManagers.ScreenFakeUpdateManager)
        assertEquals(4, info(manager).updatePriority())
    }

    @Test
    fun `no extra, or a release build, gets Play's manager`() {
        assertFalse(UpdateManagers.create(activity(null), debug = true) is UpdateManagers.ScreenFakeUpdateManager)
        assertFalse(UpdateManagers.create(activity(4), debug = false) is UpdateManagers.ScreenFakeUpdateManager)
    }

    @Test
    fun `fake flow opens a screen through the launcher`() {
        val activity = activity(4)
        val manager = UpdateManagers.create(activity, debug = true)
        val launcher = activity.activityResultRegistry.register("u", ActivityResultContracts.StartIntentSenderForResult()) {}
        val info = info(manager)
        assertTrue(manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE)))
    }
}
