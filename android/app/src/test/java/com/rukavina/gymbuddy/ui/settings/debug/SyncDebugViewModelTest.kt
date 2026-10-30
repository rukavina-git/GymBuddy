package com.rukavina.gymbuddy.ui.settings.debug

import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.entity.OutboxOperation
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.local.entity.SyncLogEntity
import com.rukavina.gymbuddy.data.local.entity.SyncLogOutcome
import com.rukavina.gymbuddy.data.sync.SyncException
import com.rukavina.gymbuddy.data.sync.SyncLocalStore
import com.rukavina.gymbuddy.data.sync.SyncScheduler
import com.rukavina.gymbuddy.domain.sync.SyncOutcome
import com.rukavina.gymbuddy.domain.sync.SyncReason
import com.rukavina.gymbuddy.domain.sync.SyncRequester
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncDebugViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private lateinit var db: AppDatabase
    private lateinit var store: SyncLocalStore

    private class FakeRequester(private val outcome: SyncOutcome = SyncOutcome.SYNCED) : SyncRequester {
        val reasons = mutableListOf<SyncReason>()
        override fun requestSync(reason: SyncReason) {
            reasons += reason
        }
        override suspend fun syncNow(reason: SyncReason): SyncOutcome {
            reasons += reason
            return outcome
        }
    }

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = SyncLocalStore(db, clock)
    }

    private val viewModels = mutableListOf<SyncDebugViewModel>()

    /** uiState keeps Room collectors alive for 5s after the last subscriber; stop them before Main is reset. */
    @After
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        db.close()
    }

    private fun viewModel(scheduler: SyncScheduler = scheduler(), requester: SyncRequester = FakeRequester()) =
        SyncDebugViewModel(store, scheduler, requester).also { viewModels += it }

    private fun scheduler(runCycle: suspend () -> Unit = {}) = SyncScheduler(
        runCycle = runCycle,
        outboxCount = { 0 },
        isSignedIn = { true },
        scope = CoroutineScope(Dispatchers.Unconfined),
        now = { 1_000L }
    )

    /** Room emits off the main dispatcher, so wait for the state to match. */
    private fun SyncDebugViewModel.awaitState(predicate: (SyncDebugUiState) -> Boolean) = runBlocking {
        withTimeout(5_000) { uiState.first(predicate) }
    }

    @Test
    fun `empty state before anything is queued or synced`() {
        val vm = viewModel()

        val state = vm.awaitState { true }

        assertTrue(state.isEmpty)
        assertTrue(state.outboxCounts.isEmpty())
        assertNull(state.cursor)
        assertNull(state.status.lastOutcome)
    }

    @Test
    fun `outbox is counted by entity type`() = runBlocking {
        val dao = db.syncDao()
        dao.enqueue(SyncEntityType.EXERCISE, "e1", OutboxOperation.UPSERT, 1)
        dao.enqueue(SyncEntityType.EXERCISE, "e2", OutboxOperation.DELETE, 2)
        dao.enqueue(SyncEntityType.WORKOUT_SESSION, "s1", OutboxOperation.UPSERT, 3)
        val vm = viewModel()

        val state = vm.awaitState { it.outbox.size == 3 }

        assertEquals(mapOf(SyncEntityType.WORKOUT_SESSION to 1, SyncEntityType.EXERCISE to 2), state.outboxCounts)
        assertEquals(listOf("e1", "e2", "s1"), state.outbox.map { it.entityId })
        assertFalse(state.isEmpty)
    }

    @Test
    fun `cursor is shortened and discarded changes are listed newest first`() = runBlocking {
        store.setCursor("abcdefgh-0123456789-ijklmnop")
        db.syncDao().insertLog(SyncLogEntity(0, SyncEntityType.EXERCISE, "e1", SyncLogOutcome.CONFLICT, null, null, 1))
        db.syncDao().insertLog(SyncLogEntity(0, SyncEntityType.EXERCISE, "e2", SyncLogOutcome.PULL_OVERWRITE, null, null, 2))
        val vm = viewModel()

        val state = vm.awaitState { it.cursor != null && it.discarded.size == 2 }

        assertEquals("abcdefgh…ijklmnop", state.cursor)
        assertEquals(listOf("e2", "e1"), state.discarded.map { it.entityId })
    }

    @Test
    fun `short cursor is shown whole`() {
        assertEquals("abc", SyncDebugViewModel.shorten("abc"))
    }

    @Test
    fun `last error and its trace id come from the scheduler`() = runBlocking {
        val scheduler = scheduler { throw SyncException("pull failed: HTTP 500", 500, traceId = "trace-42") }
        val vm = viewModel(scheduler)

        scheduler.syncNow(SyncReason.MANUAL)
        val status = vm.awaitState { it.status.lastOutcome != null }.status

        assertEquals(SyncOutcome.FAILED, status.lastOutcome)
        assertEquals("pull failed: HTTP 500", status.lastError)
        assertEquals("trace-42", status.lastTraceId)
        assertEquals(1_000L, status.lastSyncAt)
    }

    @Test
    fun `sync now goes through the requester`() {
        val requester = FakeRequester()
        val vm = viewModel(requester = requester)

        vm.syncNow()

        assertEquals(listOf(SyncReason.MANUAL), requester.reasons)
        assertFalse(vm.awaitState { !it.isSyncing }.signedOut)
    }

    @Test
    fun `sync now while signed out says so`() {
        val vm = viewModel(requester = FakeRequester(SyncOutcome.SIGNED_OUT))

        vm.syncNow()

        assertTrue(vm.awaitState { it.signedOut }.signedOut)
    }
}
