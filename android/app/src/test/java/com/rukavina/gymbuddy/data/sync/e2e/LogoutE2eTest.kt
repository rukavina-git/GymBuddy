package com.rukavina.gymbuddy.data.sync.e2e

import com.rukavina.gymbuddy.data.sync.FakeAuthSession
import com.rukavina.gymbuddy.data.sync.LogoutCoordinator
import com.rukavina.gymbuddy.data.sync.LogoutResult
import com.rukavina.gymbuddy.data.sync.SyncFixtures
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Scenario 12, the successful-flush outcome, against the real backend. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LogoutE2eTest {

    private val devices = mutableListOf<TestDevice>()

    @Before
    fun requireBackend() = E2eBackend.assumeConfigured()

    @After
    fun tearDown() = devices.forEach { it.close() }

    @Test
    fun `a non-empty outbox is flushed to the server, then logout proceeds immediately`() = runBlocking<Unit> {
        val uid = E2eBackend.newUid("s12")
        val phone = TestDevice(uid).also { devices += it }
        val session = SyncFixtures.session(title = "Logged just before logout")
        phone.sessions.createWorkoutSession(session)
        val engine = phone.engine()
        val auth = FakeAuthSession(uid)

        val result = LogoutCoordinator(phone.store, { engine.flushOutbox() }, auth).requestLogout()

        assertEquals(LogoutResult.LoggedOut, result)
        assertEquals(1, auth.signOuts)
        assertEquals(0, phone.syncDao.outboxCount())
        assertNull("local user data wiped", phone.syncDao.getSessionIncludingDeleted(session.id))

        // The flush really reached the server before the wipe.
        val otherDevice = TestDevice(uid).also { devices += it }
        otherDevice.engine().pull()
        val onServer = otherDevice.syncDao.getSessionIncludingDeleted(session.id)
        assertNotNull(onServer)
        assertEquals("Logged just before logout", onServer!!.title)
    }
}
