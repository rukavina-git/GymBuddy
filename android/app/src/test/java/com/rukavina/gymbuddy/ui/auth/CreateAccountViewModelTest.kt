package com.rukavina.gymbuddy.ui.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.auth.AuthError
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CreateAccountViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val main = MainDispatcherRule(dispatcher)

    private val clock = Clock.fixed(Instant.ofEpochMilli(1_000L), ZoneOffset.UTC)
    // DataStore on an unconfined scope: writes land before the next line runs.
    private val preferences = AppPreferencesRepository(
        PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())) {
            File.createTempFile("create", ".preferences_pb").apply { delete(); deleteOnExit() }
        }
    )
    private val client = FakeAuthClient()
    private val created = mutableListOf<CreateAccountViewModel>()

    private fun vm(from: CreateFrom = CreateFrom.WELCOME) = CreateAccountViewModel(
        SavedStateHandle(mapOf(CreateAccountViewModel.ARG_FROM to from.name)), client, preferences, clock,
    ).also { created += it }

    @After
    fun cancelViewModels() = created.forEach { it.viewModelScope.cancel() }

    private fun CreateAccountViewModel.collectEvents(scope: CoroutineScope): MutableList<CreateAccountEvent> {
        val out = mutableListOf<CreateAccountEvent>()
        scope.launch(dispatcher) { events.collect { out += it } }
        return out
    }

    private fun CreateAccountViewModel.fillEmail(email: String = "a@example.com", password: String = "longenough") {
        chooseEmail()
        onEmail(email)
        onPassword(password)
        continueWithEmail()
    }

    @Test
    fun `email path walks method, email, consent and creates nothing before Agree`() = runTest(dispatcher) {
        val vm = vm()
        assertEquals(CreateStep.METHOD, vm.state.value.step)
        vm.fillEmail()
        assertEquals(CreateStep.CONSENT, vm.state.value.step)
        assertTrue(client.calls.isEmpty())
        assertNull(preferences.pendingConsentAt.first())
    }

    @Test
    fun `the email form checks before moving on`() = runTest(dispatcher) {
        val vm = vm()
        vm.fillEmail(email = "", password = "")
        assertEquals(FormError.EMPTY, vm.state.value.error)
        vm.fillEmail(email = "nope", password = "longenough")
        assertEquals(FormError.INVALID_EMAIL, vm.state.value.error)
        vm.fillEmail(password = "short")
        assertEquals(FormError.WEAK_PASSWORD, vm.state.value.error)
        assertEquals(CreateStep.EMAIL, vm.state.value.step)
    }

    @Test
    fun `agree parks the consent, then creates the account`() = runTest(dispatcher) {
        client.onCall = { assertEquals(1_000L, preferences.pendingConsentAt.first()) }
        val vm = vm()
        val events = vm.collectEvents(backgroundScope)
        vm.fillEmail()
        vm.agree()

        assertEquals(listOf("create a@example.com"), client.calls)
        assertEquals(listOf(CreateAccountEvent.Created(newAccount = true)), events)
        assertEquals(1_000L, preferences.pendingConsentAt.first())
        assertEquals("a@example.com", preferences.lastSignInEmail.first())
    }

    @Test
    fun `email in use goes back to the email screen and drops the consent`() = runTest(dispatcher) {
        client.failWith = AuthError.EMAIL_IN_USE
        val vm = vm()
        vm.fillEmail()
        vm.agree()

        assertEquals(CreateStep.EMAIL, vm.state.value.step)
        assertEquals(FormError.EMAIL_IN_USE, vm.state.value.error)
        assertNull(preferences.pendingConsentAt.first())
        assertFalse(vm.state.value.busy)
    }

    @Test
    fun `offline stays on consent and Retry agrees again`() = runTest(dispatcher) {
        client.failWith = AuthError.OFFLINE
        val vm = vm()
        val events = vm.collectEvents(backgroundScope)
        vm.fillEmail()
        vm.agree()
        assertTrue(vm.state.value.offline)
        assertEquals(CreateStep.CONSENT, vm.state.value.step)

        client.failWith = null
        vm.retry()
        assertEquals(listOf(CreateAccountEvent.Created(newAccount = true)), events)
    }

    @Test
    fun `google holds the token until Agree and reports an existing account`() = runTest(dispatcher) {
        client.googleIsNew = false
        val vm = vm()
        val events = vm.collectEvents(backgroundScope)
        vm.onGoogleToken("token")
        assertEquals(CreateStep.CONSENT, vm.state.value.step)
        assertTrue(client.calls.isEmpty())

        vm.agree()
        assertEquals(listOf("google token"), client.calls)
        assertEquals(listOf(CreateAccountEvent.Created(newAccount = false)), events)
    }

    @Test
    fun `not now keeps the guest and creates nothing`() = runTest(dispatcher) {
        val vm = vm(CreateFrom.SETUP)
        val events = vm.collectEvents(backgroundScope)
        vm.fillEmail()
        vm.notNow()
        assertEquals(listOf(CreateAccountEvent.NotNow), events)
        assertTrue(client.calls.isEmpty())
        assertNull(preferences.pendingConsentAt.first())
    }

    @Test
    fun `back walks the steps and forgets the Google token`() = runTest(dispatcher) {
        val vm = vm()
        vm.onGoogleToken("token")
        assertTrue(vm.back())
        assertEquals(CreateStep.METHOD, vm.state.value.step)
        assertFalse(vm.state.value.withGoogle)

        vm.fillEmail()
        assertTrue(vm.back())
        assertEquals(CreateStep.EMAIL, vm.state.value.step)
        assertEquals("a@example.com", vm.state.value.email)
        assertTrue(vm.back())
        assertFalse(vm.back())
    }
}
