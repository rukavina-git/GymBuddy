package com.rukavina.gymbuddy.data.sync

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SecurityProviderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `installs with the given context`() {
        var got: Context? = null
        SecurityProvider.install(context) { got = it }
        assertSame(context, got)
    }

    @Test
    fun `outdated Play services does not crash`() {
        SecurityProvider.install(context) { throw GooglePlayServicesRepairableException(2, "update", Intent()) }
    }

    @Test
    fun `missing Play services does not crash`() {
        SecurityProvider.install(context) { throw GooglePlayServicesNotAvailableException(1) }
    }
}
