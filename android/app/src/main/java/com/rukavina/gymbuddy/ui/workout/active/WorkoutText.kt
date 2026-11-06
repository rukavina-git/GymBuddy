package com.rukavina.gymbuddy.ui.workout.active

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.ui.design.KeypadValue
import com.rukavina.gymbuddy.ui.template.clock
import com.rukavina.gymbuddy.ui.workout.RestState
import com.rukavina.gymbuddy.ui.workout.SetField
import com.rukavina.gymbuddy.ui.workout.SetFields
import com.rukavina.gymbuddy.ui.workout.SetRule
import com.rukavina.gymbuddy.ui.workout.WorkoutSet
import com.rukavina.gymbuddy.utils.UnitConverter
import java.text.NumberFormat

/** Display text for the workout screen and summary (Plan 7 06a). */

/** "kg" / "lbs", "reps", "" (time is m:ss), "m". */
@Composable
internal fun unitText(field: SetField, units: PreferredUnits, value: String = ""): String =
    unitText(field, units, value) { stringResource(it) }

/** [unitText] outside Compose (the workout notification). */
internal fun unitText(res: Resources, field: SetField, units: PreferredUnits, value: String = ""): String =
    unitText(field, units, value) { res.getString(it) }

private inline fun unitText(field: SetField, units: PreferredUnits, value: String, string: (Int) -> String): String =
    when (field) {
        SetField.Weight -> UnitConverter.getWeightUnitLabel(units)
        SetField.Reps -> string(if (value == "1") R.string.workout_unit_rep else R.string.workout_unit_reps)
        SetField.Duration -> ""
        SetField.Distance -> string(R.string.workout_unit_metres)
    }

@Composable
internal fun fieldWord(field: SetField): String = stringResource(
    when (field) {
        SetField.Weight -> R.string.workout_field_weight
        SetField.Reps -> R.string.workout_field_reps
        SetField.Duration -> R.string.workout_field_time
        SetField.Distance -> R.string.workout_field_distance
    },
)

/** The number a panel shows: "82.5", "8", "1:00". Empty weight reads 0. */
internal fun panelNumber(set: WorkoutSet, field: SetField): String {
    val text = SetFields.text(set, field)
    return when (field) {
        SetField.Duration -> clock(text.toIntOrNull() ?: 0)
        else -> text.ifEmpty { "0" }
    }
}

/** "± 2.5 kg", "± 1 rep", "± 5 s", "± 100 m". */
@Composable
internal fun stepText(field: SetField, units: PreferredUnits): String {
    val step = SetFields.format(SetFields.step(field, units))
    val unit = when (field) {
        SetField.Duration -> stringResource(R.string.workout_unit_seconds)
        else -> unitText(field, units, step)
    }
    return stringResource(R.string.workout_step, "$step $unit")
}

/** A logged or upcoming set as one line: "80 kg × 8", "8 reps", "1:00", "400 m · 1:30". */
@Composable
internal fun setValueText(type: ExerciseTrackingType, set: WorkoutSet, units: PreferredUnits): String =
    setValueText(type, set) { field, number -> unitText(field, units, number) }

/** [setValueText] outside Compose (the workout notification). */
internal fun setValueText(res: Resources, type: ExerciseTrackingType, set: WorkoutSet, units: PreferredUnits): String =
    setValueText(type, set) { field, number -> unitText(res, field, units, number) }

private inline fun setValueText(type: ExerciseTrackingType, set: WorkoutSet, unitOf: (SetField, String) -> String): String {
    val fields = SetFields.of(type)
    val parts = fields.map { field ->
        val number = panelNumber(set, field)
        // "80 kg × 8": reps after a weight go without their word.
        val unit = if (field == SetField.Reps && SetField.Weight in fields) "" else unitOf(field, number)
        if (unit.isEmpty()) number else "$number $unit"
    }
    val joiner = if (type == ExerciseTrackingType.DISTANCE_DURATION) " · " else " × "
    return parts.joinToString(joiner)
}

/** Header target: "4 × 8", "3 × 1:00", or just "4 sets" without a target. */
internal fun targetText(type: ExerciseTrackingType, sets: Int, reps: Int?): String? {
    if (SetField.Reps !in SetFields.of(type) || reps == null) return null
    return "$sets × $reps"
}

/** "1:24 rest" while it runs, "2:00 rest · +0:45" once it has passed. */
@Composable
internal fun restText(rest: RestState, elapsedSeconds: Long): String {
    val left = rest.seconds - (elapsedSeconds - rest.startedAtElapsed)
    return if (left > 0) {
        stringResource(R.string.workout_rest_left, clock(left.toInt()))
    } else {
        stringResource(R.string.workout_rest_over, clock(rest.seconds), clock((-left).toInt()))
    }
}

@Composable
internal fun ruleText(rule: SetRule): String = stringResource(
    when (rule) {
        SetRule.AtLeastOneRep -> R.string.workout_rule_rep
        SetRule.AtLeastOneSecond -> R.string.workout_rule_second
        SetRule.OverZeroDistance -> R.string.workout_rule_distance
    },
)

/** The keypad's value while typing: clock digits as m:ss. */
internal fun keypadDisplay(field: SetField, text: String): String =
    if (field.kind == KeypadValue.Kind.Clock) KeypadValue.clockText(text) else text

/** "9,120" in the user's locale, whole numbers. */
internal fun wholeNumber(value: Float): String = NumberFormat.getIntegerInstance().format(Math.round(value))
