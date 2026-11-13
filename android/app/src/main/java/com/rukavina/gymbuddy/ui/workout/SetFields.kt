package com.rukavina.gymbuddy.ui.workout

import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.ui.design.KeypadValue
import java.math.BigDecimal

/** One value on a set card, in keypad Next order (Figma 53:2686: kg, then reps). */
enum class SetField(val kind: KeypadValue.Kind) {
    Weight(KeypadValue.Kind.Decimal),
    Reps(KeypadValue.Kind.Whole),
    Duration(KeypadValue.Kind.Clock),
    Distance(KeypadValue.Kind.Decimal),
}

/** Why a value can't be logged (Figma 53:2873 "Keypad rules"). */
enum class SetRule { OverZeroWeight, AtLeastOneRep, AtLeastOneSecond, OverZeroDistance }

/**
 * The set card's values per tracking type: which fields, − / + steps and
 * bounds, and the rules a set must meet before Done (Plan 7 06a). Set
 * text is what the user typed, in display units; durations are seconds.
 */
object SetFields {

    private const val MAX_WEIGHT = 999f
    private const val MAX_REPS = 999f
    private const val MAX_DURATION = 5999f // 99:59
    private const val MAX_DISTANCE = 99_999f

    fun of(type: ExerciseTrackingType): List<SetField> = when (type) {
        ExerciseTrackingType.WEIGHT_REPS -> listOf(SetField.Weight, SetField.Reps)
        ExerciseTrackingType.REPS_ONLY -> listOf(SetField.Reps)
        ExerciseTrackingType.DURATION -> listOf(SetField.Duration)
        ExerciseTrackingType.WEIGHT_DURATION -> listOf(SetField.Weight, SetField.Duration)
        ExerciseTrackingType.DISTANCE_DURATION -> listOf(SetField.Distance, SetField.Duration)
        ExerciseTrackingType.WEIGHT_DISTANCE -> listOf(SetField.Weight, SetField.Distance)
    }

    fun text(set: WorkoutSet, field: SetField): String = when (field) {
        SetField.Weight -> set.weight
        SetField.Reps -> set.reps
        SetField.Duration -> set.duration
        SetField.Distance -> set.distance
    }

    fun with(set: WorkoutSet, field: SetField, text: String): WorkoutSet = when (field) {
        SetField.Weight -> set.copy(weight = text)
        SetField.Reps -> set.copy(reps = text)
        SetField.Duration -> set.copy(duration = text)
        SetField.Distance -> set.copy(distance = text)
    }

    /** ± 2.5 kg (5 lb), ± 1 rep, ± 5 s, ± 100 m. */
    fun step(field: SetField, units: PreferredUnits): Float = when (field) {
        SetField.Weight -> if (units == PreferredUnits.IMPERIAL) 5f else 2.5f
        SetField.Reps -> 1f
        SetField.Duration -> 5f
        SetField.Distance -> 100f
    }

    /** Lowest value − reaches: 0 kg, 1 rep, 5 s, 0 m (Figma 53:2796 "Limits"). */
    fun floor(field: SetField): Float = when (field) {
        SetField.Weight, SetField.Distance -> 0f
        SetField.Reps -> 1f
        SetField.Duration -> 5f
    }

    fun ceiling(field: SetField): Float = when (field) {
        SetField.Weight -> MAX_WEIGHT
        SetField.Reps -> MAX_REPS
        SetField.Duration -> MAX_DURATION
        SetField.Distance -> MAX_DISTANCE
    }

    fun value(set: WorkoutSet, field: SetField): Float? = KeypadValue.parse(text(set, field))

    /** − (plus = false) or + on [field]; null when already at the bound. Empty counts as 0. */
    fun stepped(set: WorkoutSet, field: SetField, plus: Boolean, units: PreferredUnits): WorkoutSet? {
        val current = value(set, field)
        val step = step(field, units)
        val base = current ?: 0f
        val next = (if (plus) base + step else base - step).coerceIn(floor(field), ceiling(field))
        if (current != null && next == current) return null
        return with(set, field, format(next))
    }

    fun canStep(set: WorkoutSet, field: SetField, plus: Boolean): Boolean {
        val current = value(set, field) ?: return true
        return if (plus) current < ceiling(field) else current > floor(field)
    }

    /**
     * The rule [text] breaks for [field], or null. Weight must be over 0
     * (F17: bodyweight lifts use Reps only) unless [weightRequired] is
     * false: editing past sessions, logged when weight was optional.
     */
    fun rule(field: SetField, text: String, weightRequired: Boolean = true): SetRule? {
        val v = KeypadValue.parse(text)
        return when (field) {
            SetField.Weight -> if (weightRequired && (v == null || v <= 0f)) SetRule.OverZeroWeight else null
            SetField.Reps -> if (v == null || v < 1f) SetRule.AtLeastOneRep else null
            SetField.Duration -> if (v == null || v < 1f) SetRule.AtLeastOneSecond else null
            SetField.Distance -> if (v == null || v <= 0f) SetRule.OverZeroDistance else null
        }
    }

    /** First rule the set breaks, or null when Done may log it. */
    fun rule(type: ExerciseTrackingType, set: WorkoutSet, weightRequired: Boolean = true): SetRule? =
        of(type).firstNotNullOfOrNull { rule(it, text(set, it), weightRequired) }

    /**
     * Whether a done set is saved. Skips the weight rule: sets logged
     * before F17 (empty or 0 kg, e.g. in a restored draft) must not be lost.
     */
    fun savable(type: ExerciseTrackingType, set: WorkoutSet): Boolean =
        rule(type, set, weightRequired = false) == null

    /** Keypad text → stored form: clock digits to seconds, "," to ".", "" when empty. */
    fun stored(kind: KeypadValue.Kind, text: String): String = when (kind) {
        KeypadValue.Kind.Clock -> KeypadValue.parseClock(text)?.toString().orEmpty()
        KeypadValue.Kind.Whole -> KeypadValue.parseWhole(text)?.toString().orEmpty()
        KeypadValue.Kind.Decimal -> KeypadValue.parse(text)?.let(::format).orEmpty()
    }

    /** 82.5 → "82.5", 80.0 → "80", 1.25 → "1.25". Always "." (the stored form). */
    fun format(value: Float): String =
        BigDecimal(value.toString()).stripTrailingZeros().toPlainString()
}
