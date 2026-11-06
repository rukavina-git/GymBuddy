package com.rukavina.gymbuddy.ui.you.body

import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.domain.model.BodyWeightEntry
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.repository.BodyWeightRepository
import com.rukavina.gymbuddy.ui.design.KeypadInput
import com.rukavina.gymbuddy.testutil.FixedIdGenerator
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import com.rukavina.gymbuddy.testutil.testPreferencesDataStore
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class BodyWeightViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-02T09:00:00Z"), ZoneOffset.UTC)

    private class FakeRepo : BodyWeightRepository {
        val entries = MutableStateFlow<List<BodyWeightEntry>>(emptyList())
        val saved = mutableListOf<BodyWeightEntry>()
        val deleted = mutableListOf<String>()
        override fun observeEntries(): Flow<List<BodyWeightEntry>> = entries
        override suspend fun save(entry: BodyWeightEntry) { saved += entry }
        override suspend fun delete(id: String) { deleted += id }
    }

    private val repo = FakeRepo()
    private val prefs = AppPreferencesRepository(testPreferencesDataStore())

    private fun vm(units: PreferredUnits = PreferredUnits.METRIC): BodyWeightViewModel = runBlocking {
        prefs.setPreferredUnits(units)
        BodyWeightViewModel(repo, prefs, FixedIdGenerator("new"), clock).also { it.zone = ZoneOffset.UTC }.also { v ->
            waitUntil { !v.uiState.value.loading && v.uiState.value.units == units }
        }
    }

    private fun waitUntil(condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 2_000
        while (!condition()) {
            check(System.currentTimeMillis() < end) { "timed out" }
            Thread.sleep(5)
        }
    }

    @Test
    fun `log starts at the latest weight, else 70 kg`() {
        val v = vm()
        v.openLog()
        assertEquals(70f, v.log.value!!.value)
        repo.entries.value = listOf(BodyWeightEntry("a", 1, 82.4f))
        waitUntil { v.uiState.value.latest != null }
        v.openLog()
        assertEquals(82.4f, v.log.value!!.value)
        assertEquals(LocalDate.of(2026, 11, 2), v.log.value!!.date)
    }

    @Test
    fun `steps are a tenth and stop at the floor`() {
        val v = vm()
        v.openLog()
        v.step(up = true); v.step(up = true)
        assertEquals(70.2f, v.log.value!!.value)
        v.step(up = false)
        assertEquals(70.1f, v.log.value!!.value)
        repeat(600) { v.step(up = false) }
        assertEquals(BodyWeightViewModel.MIN_KG, v.log.value!!.value)
        assertFalse(v.canStep(up = false))
        assertTrue(v.canStep(up = true))
    }

    @Test
    fun `keypad commits a typed number, rejects one out of range, cancel keeps the old`() {
        val v = vm()
        v.openLog()
        v.openKeypad()
        listOf(KeypadInput.Digit(8), KeypadInput.Digit(1), KeypadInput.Decimal, KeypadInput.Digit(5)).forEach(v::keypadInput)
        assertEquals("81.5", v.log.value!!.keypad)
        v.keypadInput(KeypadInput.Next)
        assertEquals(81.5f, v.log.value!!.value)
        assertNull(v.log.value!!.keypad)

        v.openKeypad()
        listOf(KeypadInput.Digit(9), KeypadInput.Digit(9), KeypadInput.Digit(9), KeypadInput.Digit(9)).forEach(v::keypadInput)
        v.keypadInput(KeypadInput.Next)
        assertTrue(v.log.value!!.keypadError)
        v.keypadInput(KeypadInput.Cancel)
        assertEquals(81.5f, v.log.value!!.value)
        assertNull(v.log.value!!.keypad)
    }

    @Test
    fun `save today stores now in kg, an earlier day at noon, lbs converted`() {
        val v = vm(PreferredUnits.IMPERIAL)
        v.openLog()
        assertEquals(154.3f, v.log.value!!.value)
        v.save()
        waitUntil { repo.saved.size == 1 }
        assertEquals(clock.millis(), repo.saved[0].measuredAt)
        assertEquals(70f, repo.saved[0].weightKg, 0.05f)
        assertEquals("new", repo.saved[0].id)
        assertNull(v.log.value)

        v.openLog()
        v.setDate(LocalDate.of(2026, 11, 3)) // future: ignored
        assertEquals(LocalDate.of(2026, 11, 2), v.log.value!!.date)
        v.setDate(LocalDate.of(2026, 10, 30))
        v.save()
        waitUntil { repo.saved.size == 2 }
        assertEquals(LocalDate.of(2026, 10, 30).atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC).toEpochMilli(), repo.saved[1].measuredAt)
    }

    @Test
    fun `delete tombstones and offers undo, undo saves the same entry`() = runBlocking {
        val v = vm()
        val entry = BodyWeightEntry("a", 5, 80f)
        val event = async(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) { v.events.first() }
        v.delete(entry)
        assertEquals(BodyWeightEvent.Deleted(entry), withTimeout(2_000) { event.await() })
        assertEquals(listOf("a"), repo.deleted)
        v.undoDelete(entry)
        waitUntil { repo.saved == listOf(entry) }
    }

    @Test
    fun `range switches`() {
        val v = vm()
        v.setRange(WeightRange.Year)
        waitUntil { v.uiState.value.range == WeightRange.Year }
    }
}
