package com.rukavina.gymbuddy.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Points viewModelScope's Dispatchers.Main at a test dispatcher for the
 * duration of a test, so `init {}` blocks and viewModelScope.launch calls
 * run without a real Android main looper.
 *
 * Unconfined rather than Standard: every ViewModel under test here starts
 * long-lived collectors in init (e.g. appPreferencesRepository.preferredUnits)
 * that must not block the rest of init from running - Standard would
 * require manually advancing past them before every assertion.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
