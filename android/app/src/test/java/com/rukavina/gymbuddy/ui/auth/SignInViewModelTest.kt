package com.rukavina.gymbuddy.ui.auth

import com.rukavina.gymbuddy.data.auth.AuthError
import com.rukavina.gymbuddy.data.auth.SignInSupport
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Rule
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import org.junit.Test

class SignInViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val main = MainDispatcherRule(dispatcher)

    // DataStore on an unconfined scope: writes land before the next line runs.
    private val preferences = AppPreferencesRepository(
        PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())) {
            File.createTempFile("signin", ".preferences_pb").apply { delete(); deleteOnExit() }
        }
    )
    private val client = FakeAuthClient()
    private val support = FakeSignInSupport()

    private val created = mutableListOf<SignInViewModel>()

    private fun vm() = SignInViewModel(client, support, preferences).also { created += it }

    // init's DataStore read can still be in flight when a test ends; left
    // alone it resumes on Main after resetMain and fails a later test.
    @After
    fun cancelViewModels() = created.forEach { it.viewModelScope.cancel() }

    private fun SignInViewModel.collectEvents(scope: kotlinx.coroutines.CoroutineScope): MutableList<SignInEvent> {
        val out = mutableListOf<SignInEvent>()
        scope.launch(dispatcher) { events.collect { out += it } }
        return out
    }

    @Test
    fun `email sign-in goes in and remembers the address`() = runTest(dispatcher) {
        val vm = vm()
        val events = vm.collectEvents(backgroundScope)
        vm.onEmail("a@example.com")
        vm.onPassword("secret123")
        vm.submitEmail()

        assertEquals(listOf("email a@example.com"), client.calls)
        assertEquals(listOf(SignInEvent.Done), events)
        assertEquals("a@example.com", preferences.lastSignInEmail.first())
    }

    @Test
    fun `the last email is filled in on sign-in`() = runTest(dispatcher) {
        preferences.setLastSignInEmail("last@example.com")
        assertEquals("last@example.com", vm().state.value.email)
    }

    @Test
    fun `empty form asks for both fields without calling Firebase`() = runTest(dispatcher) {
        val vm = vm()
        vm.submitEmail()
        assertEquals(FormError.EMPTY, vm.state.value.error)
        assertTrue(client.calls.isEmpty())
    }

    @Test
    fun `a wrong email or password reads the same`() = runTest(dispatcher) {
        client.failWith = AuthError.WRONG_CREDENTIALS
        val vm = vm()
        vm.onEmail("a@example.com")
        vm.onPassword("nope")
        vm.submitEmail()
        assertEquals(FormError.WRONG_CREDENTIALS, vm.state.value.error)
        assertNull(vm.state.value.busy)
    }

    @Test
    fun `offline shows the box and Retry runs the same sign-in again`() = runTest(dispatcher) {
        client.failWith = AuthError.OFFLINE
        val vm = vm()
        val events = vm.collectEvents(backgroundScope)
        vm.onEmail("a@example.com")
        vm.onPassword("secret123")
        vm.submitEmail()
        assertTrue(vm.state.value.offline)

        client.failWith = null
        vm.retry()
        assertFalse(vm.state.value.offline)
        assertEquals(listOf("email a@example.com", "email a@example.com"), client.calls)
        assertEquals(listOf(SignInEvent.Done), events)
    }

    @Test
    fun `google with an existing account signs in for real`() = runTest(dispatcher) {
        val vm = vm()
        val events = vm.collectEvents(backgroundScope)
        vm.onGoogleToken("token", "a@gmail.com")
        assertEquals(listOf("probe token", "endProbe(false)", "google token"), client.calls)
        assertEquals(listOf(SignInEvent.Done), events)
    }

    @Test
    fun `google with no GymBuddy account asks first and keeps nothing`() = runTest(dispatcher) {
        client.probeIsNew = true
        val vm = vm()
        val events = vm.collectEvents(backgroundScope)
        vm.onGoogleToken("token", "a@gmail.com")
        assertEquals(SignInDialog.NoAccount("a@gmail.com"), vm.state.value.dialog)
        assertEquals(listOf("probe token"), client.calls)

        vm.noAccountOther()
        assertNull(vm.state.value.dialog)
        assertEquals(listOf("probe token", "endProbe(true)"), client.calls)
        assertEquals(listOf(SignInEvent.PickAnotherGoogleAccount), events)
    }

    @Test
    fun `create account from the no-account question deletes the probe first`() = runTest(dispatcher) {
        client.probeIsNew = true
        val vm = vm()
        val events = vm.collectEvents(backgroundScope)
        vm.onGoogleToken("token", "a@gmail.com")
        vm.noAccountCreate()
        assertEquals(listOf("probe token", "endProbe(true)"), client.calls)
        assertEquals(listOf(SignInEvent.OpenCreateAccount), events)
    }

    @Test
    fun `another account's waiting changes are named before signing in`() = runTest(dispatcher) {
        support.ownerEmail = "kim@gmail.com"
        support.pending = 2
        val vm = vm()
        vm.onEmail("mia@gmail.com")
        vm.onPassword("secret123")
        vm.submitEmail()

        assertEquals(SignInDialog.OtherAccount("kim@gmail.com", "mia@gmail.com", 2), vm.state.value.dialog)
        assertTrue(client.calls.isEmpty())

        vm.continueAsNew()
        assertEquals(listOf("email mia@gmail.com"), client.calls)
    }

    @Test
    fun `sign in as the old account instead fills its email`() = runTest(dispatcher) {
        support.ownerEmail = "kim@gmail.com"
        support.pending = 2
        val vm = vm()
        vm.onEmail("mia@gmail.com")
        vm.onPassword("secret123")
        vm.submitEmail()
        vm.switchToOwner()

        assertNull(vm.state.value.dialog)
        assertEquals("kim@gmail.com", vm.state.value.email)
        assertEquals("", vm.state.value.password)
        assertTrue(client.calls.isEmpty())
    }

    @Test
    fun `same account or nothing waiting signs straight in`() = runTest(dispatcher) {
        support.ownerEmail = "kim@gmail.com"
        support.pending = 2
        val vm = vm()
        vm.onEmail("KIM@gmail.com")
        vm.onPassword("secret123")
        vm.submitEmail()
        assertNull(vm.state.value.dialog)
        assertEquals(listOf("email KIM@gmail.com"), client.calls)
    }

    @Test
    fun `forgot password needs an email, then always says it was sent`() = runTest(dispatcher) {
        val vm = vm()
        val events = vm.collectEvents(backgroundScope)
        vm.forgotPassword()
        assertEquals(FormError.NEED_EMAIL_FOR_RESET, vm.state.value.error)

        client.failWith = AuthError.WRONG_CREDENTIALS
        vm.onEmail("ghost@example.com")
        vm.forgotPassword()
        assertEquals(listOf(SignInEvent.ResetSent("ghost@example.com")), events)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `google spins its button and blocks a second sheet`() = runTest(dispatcher) {
        val vm = vm()
        assertTrue(vm.onGoogleStarted())
        assertEquals(SignInAction.GOOGLE, vm.state.value.busy)
        assertFalse(vm.onGoogleStarted())
        vm.onGoogleCancelled()
        assertNull(vm.state.value.busy)
    }

    @Test
    fun `google sheet offline shows the box and Retry opens the sheet again`() = runTest(dispatcher) {
        val vm = vm()
        val events = vm.collectEvents(backgroundScope)
        vm.onGoogleFailed(offline = true)
        assertTrue(vm.state.value.offline)
        vm.retry()
        assertEquals(listOf(SignInEvent.PickAnotherGoogleAccount), events)
    }

    private class FakeSignInSupport : SignInSupport {
        override var ownerEmail: String? = null
        var pending = 0
        override suspend fun pendingChanges() = pending
    }
}
