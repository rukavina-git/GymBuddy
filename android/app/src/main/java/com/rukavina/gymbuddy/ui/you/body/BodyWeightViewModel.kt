package com.rukavina.gymbuddy.ui.you.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.domain.id.IdGenerator
import com.rukavina.gymbuddy.domain.model.BodyWeightEntry
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.repository.BodyWeightRepository
import com.rukavina.gymbuddy.ui.design.KeypadInput
import com.rukavina.gymbuddy.ui.design.KeypadValue
import com.rukavina.gymbuddy.utils.UnitConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.roundToInt

data class BodyWeightUiState(
    val loading: Boolean = true,
    /** Every live entry, newest first. */
    val entries: List<BodyWeightEntry> = emptyList(),
    val range: WeightRange = WeightRange.Days30,
    val units: PreferredUnits = PreferredUnits.METRIC,
) {
    val latest: BodyWeightEntry? get() = entries.firstOrNull()
}

/**
 * Log weight sheet (R15-6). [value] is in the display unit, one decimal.
 * [keypad] is the typed text while the keypad is open, else null.
 */
data class LogState(
    val value: Float,
    val date: LocalDate,
    val keypad: String? = null,
    /** The typed number is out of range (keypad stays open). */
    val keypadError: Boolean = false,
)

sealed interface BodyWeightEvent {
    data class Deleted(val entry: BodyWeightEntry) : BodyWeightEvent
    data object SaveFailed : BodyWeightEvent
}

/** Body weight page + Log sheet (7.10b). Writes go through [BodyWeightRepository]. */
@HiltViewModel
class BodyWeightViewModel @Inject constructor(
    private val repository: BodyWeightRepository,
    preferences: AppPreferencesRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
) : ViewModel() {

    /** Days follow the phone's zone; the injected Clock is UTC. */
    internal var zone: ZoneId = ZoneId.systemDefault()

    private val range = MutableStateFlow(WeightRange.Days30)

    val uiState: StateFlow<BodyWeightUiState> = combine(
        repository.observeEntries().catch { emit(emptyList()) },
        range,
        preferences.preferredUnits,
    ) { entries, r, units -> BodyWeightUiState(false, entries, r, units) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, BodyWeightUiState())

    private val _log = MutableStateFlow<LogState?>(null)
    val log: StateFlow<LogState?> = _log.asStateFlow()

    private val _events = MutableSharedFlow<BodyWeightEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<BodyWeightEvent> = _events.asSharedFlow()

    fun setRange(r: WeightRange) { range.value = r }

    private val units get() = uiState.value.units
    private fun today(): LocalDate = Instant.ofEpochMilli(clock.millis()).atZone(zone).toLocalDate()

    private fun bounds(): ClosedFloatingPointRange<Float> =
        if (units == PreferredUnits.IMPERIAL) UnitConverter.kgToLbs(MIN_KG)..UnitConverter.kgToLbs(MAX_KG) else MIN_KG..MAX_KG

    /** Starts at the latest weight, else 70 kg (154.3 lbs). */
    fun openLog() {
        val kg = uiState.value.latest?.weightKg ?: DEFAULT_KG
        _log.value = LogState(value = round1(toDisplay(kg)), date = today())
    }

    fun closeLog() { _log.value = null }

    /** ± 0.1 in the display unit, held inside the range. */
    fun step(up: Boolean) = _log.update { s ->
        s?.copy(value = round1(s.value + if (up) STEP else -STEP).coerceIn(bounds().start, bounds().endInclusive))
    }

    fun canStep(up: Boolean): Boolean {
        val s = _log.value ?: return false
        return if (up) s.value + STEP <= bounds().endInclusive + EPS else s.value - STEP >= bounds().start - EPS
    }

    fun openKeypad() = _log.update { it?.copy(keypad = "", keypadError = false) }

    fun keypadInput(input: KeypadInput) = _log.update { s ->
        val text = s?.keypad ?: return@update s
        when (input) {
            KeypadInput.Cancel -> s.copy(keypad = null, keypadError = false)
            KeypadInput.Next -> {
                val typed = KeypadValue.parse(text)
                when {
                    typed == null -> s.copy(keypad = null, keypadError = false)
                    typed !in bounds() -> s.copy(keypadError = true)
                    else -> s.copy(value = round1(typed), keypad = null, keypadError = false)
                }
            }
            else -> s.copy(keypad = KeypadValue.apply(text, input, KeypadValue.Kind.Decimal), keypadError = false)
        }
    }

    /** Future days are ignored. */
    fun setDate(date: LocalDate) = _log.update { s -> if (date.isAfter(today())) s else s?.copy(date = date) }

    /** Today: now. Another day: noon that day, so it sorts by day. */
    fun save() {
        val s = _log.value ?: return
        val kg = if (units == PreferredUnits.IMPERIAL) UnitConverter.lbsToKg(s.value) else s.value
        val at = if (s.date == today()) clock.millis() else s.date.atTime(LocalTime.NOON).atZone(zone).toInstant().toEpochMilli()
        _log.value = null
        viewModelScope.launch {
            runCatching { repository.save(BodyWeightEntry(ids.newId(), at, kg)) }
                .onFailure { _events.tryEmit(BodyWeightEvent.SaveFailed) }
        }
    }

    /** Tombstones at once; the page offers Undo. */
    fun delete(entry: BodyWeightEntry) {
        viewModelScope.launch {
            runCatching { repository.delete(entry.id) }
                .onSuccess { _events.tryEmit(BodyWeightEvent.Deleted(entry)) }
        }
    }

    /** Saves the same id again: the upsert clears the tombstone. */
    fun undoDelete(entry: BodyWeightEntry) {
        viewModelScope.launch { runCatching { repository.save(entry) } }
    }

    private fun toDisplay(kg: Float) = if (units == PreferredUnits.IMPERIAL) UnitConverter.kgToLbs(kg) else kg

    companion object {
        const val STEP = 0.1f
        const val MIN_KG = 20f
        const val MAX_KG = 700f
        const val DEFAULT_KG = 70f
        private const val EPS = 0.0001f

        fun round1(v: Float): Float = (v * 10f).roundToInt() / 10f
    }
}
