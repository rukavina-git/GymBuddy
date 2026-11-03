package com.rukavina.gymbuddy.ui.auth

import androidx.lifecycle.SavedStateHandle
import com.rukavina.gymbuddy.data.auth.AuthClient
import com.rukavina.gymbuddy.data.auth.AuthError
import com.rukavina.gymbuddy.data.auth.AuthException
import com.rukavina.gymbuddy.data.auth.GoogleProbe
import com.rukavina.gymbuddy.data.auth.SignInSupport
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.sync.LocalDataCounts
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
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SignInViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val main = MainDispatcherRule(dispatcher)

    private val clock = Clock.fixed(Instant.ofEpochMilli(1_000L), ZoneOffset.UTC)
    // DataStore on an unconfined scope: writes land before the next line runs.
    private val preferences = AppPreferencesRepository(
        PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())) {
            File.createTempFile("signin", ".preferences_pb").apply { delete(); deleteOnExit() }
        }
    )
    private val client = FakeAuthClient()
    private val support = FakeSignInSupport()

    private fun vm(mode: SignInMode = SignInMode.SIGN_IN) = SignInViewModel(
        SavedStateHandle(mapOf(SignInViewModel.ARG_MODE to mode.name)),
        client, support, preferences, clock,
    )

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
        assertFalse(vm.state.value.busy)
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
    fun `create without the consent box shows the reason and does nothing`() = runTest(dispatcher) {
        val vm = vm(SignInMode.CREATE)
        vm.onEmail("a@example.com")
        vm.onPassword("longenough")
        vm.submitEmail()
        assertTrue(vm.state.value.consentMissing)
        assertFalse(vm.canStartGoogle())
        assertTrue(client.calls.isEmpty())

        vm.onConsent(true)
        assertFalse(vm.state.value.consentMissing)
    }

    @Test
    fun `create needs 8 characters`() = runTest(dispatcher) {
        val vm = vm(SignInMode.CREATE)
        vm.onConsent(true)
        vm.onEmail("a@example.com")
        vm.onPassword("short")
        vm.submitEmail()
        assertEquals(FormError.WEAK_PASSWORD, vm.state.value.error)
        assertTrue(client.calls.isEmpty())
    }

    @Test
    fun `create parks the consent before the account appears`() = runTest(dispatcher) {
        client.onCall = { assertEquals(1_000L, preferences.pendingConsentAt.first()) }
        val vm = vm(SignInMode.CREATE)
        val events = vm.collectEvents(backgroundScope)
        vm.onConsent(true)
        vm.onEmail("a@example.com")
        vm.onPassword("longenough")
        vm.submitEmail()

        assertEquals(listOf("create a@example.com"), client.calls)
        assertEquals(listOf(SignInEvent.Done), events)
        assertEquals(1_000L, preferences.pendingConsentAt.first())
    }

    @Test
    fun `a failed create drops the pending consent`() = runTest(dispatcher) {
        client.failWith = AuthError.EMAIL_IN_USE
        val vm = vm(SignInMode.CREATE)
        vm.onConsent(true)
        vm.onEmail("a@example.com")
        vm.onPassword("longenough")
        vm.submitEmail()

        assertEquals(FormError.EMAIL_IN_USE, vm.state.value.error)
        assertNull(preferences.pendingConsentAt.first())
    }

    @Test
    fun `create shows what moves into the account`() = runTest(dispatcher) {
        support.counts = LocalDataCounts(14, 3, 2)
        assertEquals(LocalDataCounts(14, 3, 2), vm(SignInMode.CREATE).state.value.counts)
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
    fun `google in create mode skips the probe`() = runTest(dispatcher) {
        val vm = vm(SignInMode.CREATE)
        vm.onConsent(true)
        vm.onGoogleToken("token", "a@gmail.com")
        assertEquals(listOf("google token"), client.calls)
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
    fun `google sheet offline shows the box and Retry opens the sheet again`() = runTest(dispatcher) {
        val vm = vm()
        val events = vm.collectEvents(backgroundScope)
        vm.onGoogleFailed(offline = true)
        assertTrue(vm.state.value.offline)
        vm.retry()
        assertEquals(listOf(SignInEvent.PickAnotherGoogleAccount), events)
    }

    private class FakeAuthClient : AuthClient {
        val calls = mutableListOf<String>()
        var failWith: AuthError? = null
        var probeIsNew = false
        var onCall: suspend () -> Unit = {}

        private suspend fun call(name: String) {
            calls += name
            onCall()
            failWith?.let { throw AuthException(it) }
        }

        override suspend fun probeGoogle(idToken: String): GoogleProbe {
            call("probe $idToken")
            return GoogleProbe("a@gmail.com", probeIsNew)
        }

        override suspend fun endProbe(delete: Boolean) {
            calls += "endProbe($delete)"
        }

        override suspend fun signInWithGoogle(idToken: String) = call("google $idToken")
        override suspend fun signInWithEmail(email: String, password: String) = call("email $email")
        override suspend fun createWithEmail(email: String, password: String) = call("create $email")
        override suspend fun sendPasswordReset(email: String) = call("reset $email")
    }

    private class FakeSignInSupport : SignInSupport {
        override var ownerEmail: String? = null
        var pending = 0
        var counts = LocalDataCounts(0, 0, 0)
        override suspend fun pendingChanges() = pending
        override suspend fun openCounts() = counts
    }
}
