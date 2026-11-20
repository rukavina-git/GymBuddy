package com.rukavina.gymbuddy.update

import android.app.Activity
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper

/** InAppUpdates against Play's FakeAppUpdateManager. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class InAppUpdatesTest {

    private val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    private val fake = FakeAppUpdateManager(activity)
    private val launcher = activity.activityResultRegistry.register("update", ActivityResultContracts.StartIntentSenderForResult()) {}
    private val updates = InAppUpdates(activity, fake, launcher)

    private fun available(priority: Int) {
        fake.setUpdateAvailable(2002, AppUpdateType.IMMEDIATE)
        fake.setUpdatePriority(priority)
    }

    private fun checkNow() {
        updates.check()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `priority 0 shows nothing`() {
        available(0)
        checkNow()
        assertFalse(fake.isImmediateFlowVisible)
    }

    @Test
    fun `priority 4 shows the update, and backing out closes the app`() {
        available(4)
        checkNow()
        assertTrue(fake.isImmediateFlowVisible)

        updates.onResult(Activity.RESULT_CANCELED)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `priority 2 prompts once per version, and backing out keeps the app open`() {
        available(2)
        checkNow()
        assertTrue(fake.isImmediateFlowVisible)
        updates.onResult(Activity.RESULT_CANCELED)
        assertFalse(activity.isFinishing)

        fake.userRejectsUpdate()
        checkNow()
        assertFalse(fake.isImmediateFlowVisible)
    }

    @Test
    fun `no update shows nothing`() {
        fake.setUpdateNotAvailable()
        checkNow()
        assertFalse(fake.isImmediateFlowVisible)
    }
}
