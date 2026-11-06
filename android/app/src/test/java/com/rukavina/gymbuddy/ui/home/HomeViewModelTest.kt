package com.rukavina.gymbuddy.ui.home

import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.testutil.FakeTemplateSources
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import com.rukavina.gymbuddy.testutil.MutableClock
import com.rukavina.gymbuddy.testutil.TemplateFixtures.DAY
import com.rukavina.gymbuddy.testutil.TemplateFixtures.plan
import com.rukavina.gymbuddy.testutil.TemplateFixtures.session
import com.rukavina.gymbuddy.testutil.TemplateFixtures.template
import com.rukavina.gymbuddy.data.sync.Resync
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class HomeViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val clock = MutableClock() // Thu 2026-01-01 UTC
    private val now = clock.millis

    @Test
    fun rowsAreTheThreeMostRecentAndWeekCountsThisWeek() = runTest {
        val sources = FakeTemplateSources(
            templates = listOf("a", "b", "c", "d").map { template(it) },
            sessions = listOf(
                session("1", "a", now - 10 * DAY),
                session("2", "b", now - 1),
                session("3", "c", now - 2 * DAY),
                session("4", "d", now - 3 * DAY, durationSeconds = 600),
            ),
        )
        val state = HomeViewModel(sources, clock, FakeHomeSyncSignals()).uiState.first { it.loaded }
        assertEquals(listOf("b", "c", "d"), state.rows.map { it.id })
        assertEquals(LocalDate.of(2026, 1, 1), state.today)
        // Mon 29 Dec … : b (Wed 31), c (Tue 30), d (Mon 29). a was the week before.
        assertEquals(3, state.week!!.sessionCount)
    }

    @Test
    fun newUserSeesActivePlanAndEmptyRowsWithoutOne() = runTest {
        val sources = FakeTemplateSources(
            templates = listOf("a", "b").map { template(it, source = EntitySource.DEFAULT) },
            plans = listOf(plan("p", "b", "a")),
            activePlanId = "p",
        )
        val vm = HomeViewModel(sources, clock, FakeHomeSyncSignals())
        assertEquals(listOf("b", "a"), vm.uiState.first { it.loaded }.rows.map { it.id })

        sources.activePlanId.value = null
        assertEquals(emptyList<String>(), vm.uiState.first { it.loaded && it.rows.isEmpty() }.rows)
    }

    @Test
    fun startSheetListsYoursByRecencyThenStartersInPlanOrder() = runTest {
        val sources = FakeTemplateSources(
            templates = listOf(
                template("mine", source = EntitySource.CUSTOM),
                template("s1", source = EntitySource.DEFAULT),
                template("s2", source = EntitySource.DEFAULT),
                template("s3", source = EntitySource.DEFAULT),
            ),
            sessions = listOf(session("x", "s2", now - DAY)),
            plans = listOf(plan("p", "s3", "s1", "s2")),
        )
        val state = HomeViewModel(sources, clock, FakeHomeSyncSignals()).uiState.first { it.loaded }
        // Done starter counts as yours; never-done custom after it.
        assertEquals(listOf("s2", "mine"), state.sheetYours.map { it.id })
        assertEquals(listOf("s3", "s1"), state.sheetStarters.map { it.id })
        assertEquals(1, state.sheetStarters.first().planDay)
    }

    @Test
    fun backupLineShowsFromThreeDays() = runTest {
        val sync = FakeHomeSyncSignals()
        val vm = HomeViewModel(FakeTemplateSources(), clock, sync)
        assertEquals(HomeBackup.Hidden, vm.uiState.first { it.loaded }.backup)

        sync.oldest.value = now - 3 * DAY + 1
        assertEquals(HomeBackup.Hidden, vm.uiState.first { it.loaded && sync.oldest.value != null }.backup)

        sync.oldest.value = now - 3 * DAY
        assertEquals(HomeBackup.Stuck(3), vm.uiState.first { it.backup != HomeBackup.Hidden }.backup)
    }

    @Test
    fun retryOnlineBacksUpThenTheLineGoes() = runTest {
        val sync = FakeHomeSyncSignals(oldest = now - 4 * DAY)
        val vm = HomeViewModel(FakeTemplateSources(), clock, sync)
        assertEquals(HomeBackup.Stuck(4), vm.uiState.first { it.loaded }.backup)

        vm.retryBackup()
        assertEquals(HomeBackup.BackingUp, vm.uiState.first { it.backup == HomeBackup.BackingUp }.backup)
        assertEquals(1, sync.backUps)
        sync.oldest.value = null
        sync.cycle.complete(Unit)
        assertEquals(HomeBackup.Hidden, vm.uiState.first { it.backup == HomeBackup.Hidden }.backup)
    }

    @Test
    fun retryOfflineSaysNeedsInternetForFourSeconds() = runTest {
        val sync = FakeHomeSyncSignals(oldest = now - 4 * DAY, online = false)
        val vm = HomeViewModel(FakeTemplateSources(), clock, sync)
        val seen = mutableListOf<HomeBackup>()
        val job = launch { vm.uiState.collect { seen += it.backup } }
        runCurrent()

        vm.retryBackup()
        runCurrent()
        assertEquals(HomeBackup.Offline, seen.last())
        advanceTimeBy(OFFLINE_NOTICE_MS - 1)
        runCurrent()
        assertEquals(HomeBackup.Offline, seen.last())
        advanceTimeBy(2)
        runCurrent()
        assertEquals(HomeBackup.Stuck(4), seen.last())
        assertEquals(0, sync.backUps)
        job.cancel()
    }

    @Test
    fun resyncStatePassesThroughAndRetryAsksForASync() = runTest {
        val sync = FakeHomeSyncSignals()
        val vm = HomeViewModel(FakeTemplateSources(), clock, sync)
        sync.resyncState.value = Resync.Running
        assertEquals(Resync.Running, vm.uiState.first { it.resync == Resync.Running }.resync)
        sync.resyncState.value = Resync.Failed
        assertEquals(Resync.Failed, vm.uiState.first { it.resync == Resync.Failed }.resync)
        vm.retryResync()
        assertEquals(1, sync.resyncRetries)
    }

    @Test
    fun stuckDaysRule() {
        assertEquals(null, stuckDays(null, now))
        assertEquals(null, stuckDays(now - 2 * DAY, now))
        assertEquals(3, stuckDays(now - 3 * DAY, now))
        assertEquals(10, stuckDays(now - 10 * DAY - 5, now))
    }
}

private class FakeHomeSyncSignals(oldest: Long? = null, private val online: Boolean = true) : HomeSyncSignals {
    val oldest = MutableStateFlow(oldest)
    val resyncState = MutableStateFlow(Resync.Idle)
    val cycle = CompletableDeferred<Unit>()
    var backUps = 0
    var resyncRetries = 0
    override val oldestWaiting = this.oldest
    override val resync = resyncState
    override fun isOnline() = online
    override suspend fun backUpNow() { backUps++; cycle.await() }
    override fun retryResync() { resyncRetries++ }
}
