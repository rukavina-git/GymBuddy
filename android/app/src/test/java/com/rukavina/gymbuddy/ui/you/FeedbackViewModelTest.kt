package com.rukavina.gymbuddy.ui.you

import androidx.room.Room
import com.rukavina.gymbuddy.data.feedback.FEEDBACK_MAX_LENGTH
import com.rukavina.gymbuddy.data.feedback.FeedbackDatabase
import com.rukavina.gymbuddy.data.feedback.FeedbackDevice
import com.rukavina.gymbuddy.data.feedback.FeedbackOutbox
import com.rukavina.gymbuddy.data.feedback.FeedbackSubmitResult
import com.rukavina.gymbuddy.domain.id.Uuid7Generator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedbackViewModelTest {

    private lateinit var db: FeedbackDatabase
    private var online = true

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), FeedbackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun viewModel() = FeedbackViewModel(
        FeedbackOutbox(
            dao = { db.pendingFeedbackDao() },
            remote = { _, _ -> if (online) 204 else throw IOException("offline") },
            ids = Uuid7Generator(Clock.systemUTC()),
            clock = Clock.systemUTC(),
            device = FeedbackDevice("0.2.4", "Android 15", "x"),
            currentUid = { null },
            scheduleSend = {},
        )
    )

    @Test
    fun `send needs text within the cap`() {
        val vm = viewModel()
        assertFalse(vm.state.value.canSend)
        vm.onTextChange("   ")
        assertFalse(vm.state.value.canSend)
        vm.onTextChange("hi")
        assertTrue(vm.state.value.canSend)
        vm.onTextChange("x".repeat(FEEDBACK_MAX_LENGTH + 1))
        assertTrue(vm.state.value.tooLong)
        assertFalse(vm.state.value.canSend)
    }

    @Test
    fun `online send says sent`() = runTest {
        val vm = viewModel()
        vm.onTextChange("hi")
        vm.send()
        advanceUntilIdle()
        waitForResult(vm)
        assertEquals(FeedbackSubmitResult.SENT, vm.state.value.result)
    }

    @Test
    fun `offline send says saved`() = runTest {
        online = false
        val vm = viewModel()
        vm.onTextChange("hi")
        vm.send()
        waitForResult(vm)
        assertEquals(FeedbackSubmitResult.QUEUED, vm.state.value.result)
        assertFalse(vm.state.value.sending)
    }

    @Test
    fun `nothing to send does nothing`() {
        val vm = viewModel()
        vm.send()
        assertNull(vm.state.value.result)
        assertFalse(vm.state.value.sending)
    }

    /** Room's suspend calls finish on its own executor, outside the test dispatcher. */
    private fun waitForResult(vm: FeedbackViewModel) {
        val end = System.currentTimeMillis() + 5_000
        while (vm.state.value.result == null && System.currentTimeMillis() < end) Thread.sleep(10)
    }
}
