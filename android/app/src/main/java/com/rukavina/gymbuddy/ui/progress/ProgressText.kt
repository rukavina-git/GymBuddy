package com.rukavina.gymbuddy.ui.progress

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.WorkoutSet
import com.rukavina.gymbuddy.domain.progress.LiftProgress
import com.rukavina.gymbuddy.domain.progress.ProgressStats
import com.rukavina.gymbuddy.domain.progress.WeekTotals
import com.rukavina.gymbuddy.ui.exercises.page.setText
import com.rukavina.gymbuddy.ui.template.clock
import com.rukavina.gymbuddy.ui.template.hoursMinutes
import com.rukavina.gymbuddy.ui.template.joinDot
import com.rukavina.gymbuddy.utils.UnitConverter
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.YearMonth
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Text for the Progress tab and All sessions (Plan 7 step 08). */

/** kg (stored) → the display unit, as a number. */
fun displayWeight(kg: Float, units: PreferredUnits): Float =
    if (units == PreferredUnits.IMPERIAL) UnitConverter.kgToLbs(kg) else kg

/** "87.5 kg" / "193 lbs". */
@Composable
fun weightText(kg: Float, units: PreferredUnits): String =
    stringResource(R.string.weight_value, UnitConverter.weightToDisplayUnit(kg, units), UnitConverter.getWeightUnitLabel(units))

/** Totals: "9,850 kg", whole numbers grouped in the user's locale. */
@Composable
fun volumeText(kg: Float, units: PreferredUnits): String =
    stringResource(
        R.string.weight_value,
        NumberFormat.getIntegerInstance().format(displayWeight(kg, units).roundToInt()),
        UnitConverter.getWeightUnitLabel(units),
    )

/** "3 sessions · 2 h 54 m · 9,850 kg" (R7-1). */
@Composable
fun weekLine(week: WeekTotals, units: PreferredUnits): String = joinDot(
    pluralStringResource(R.plurals.progress_sessions, week.sessions, week.sessions),
    hoursMinutes(week.seconds),
    volumeText(week.volumeKg, units),
)

/** "You lift 7.5 kg more than 8 weeks ago." (R7-1, R7-5). Same when it rounds to 0 in the display unit. */
@Composable
fun liftSentence(p: LiftProgress, units: PreferredUnits): String {
    val shown = UnitConverter.weightToDisplayUnit(abs(p.change), units)
    return when {
        shown == "0" -> pluralStringResource(R.plurals.progress_same, p.weeks, p.weeks)
        p.change > 0 -> pluralStringResource(R.plurals.progress_more, p.weeks, weightText(abs(p.change), units), p.weeks)
        else -> pluralStringResource(R.plurals.progress_less, p.weeks, weightText(abs(p.change), units), p.weeks)
    }
}

/** "Heaviest set each session · 80 → 87.5 kg" (R7-1). */
@Composable
fun heaviestRange(p: LiftProgress, units: PreferredUnits): String = stringResource(
    R.string.progress_heaviest_range,
    UnitConverter.weightToDisplayUnit(p.first, units),
    weightText(p.last, units),
)

/** TalkBack summary for the chart (DESIGN.md: one stop, one sentence). */
@Composable
fun chartSummary(p: LiftProgress, units: PreferredUnits): String =
    stringResource(R.string.progress_chart_cd, liftSentence(p, units), p.lift.name, heaviestRange(p, units))

/** Card subtitle "64 min · Squat 110 kg × 5" (R7-1). */
@Composable
fun sessionSubtitle(row: SessionRow, units: PreferredUnits): String {
    val minutes = row.session.durationSeconds / 60
    val time = if (minutes > 0) stringResource(R.string.workout_minutes, minutes) else null
    val top = row.top?.let { (pe, set) -> "${pe.exerciseName} ${setText(pe.exerciseTrackingType, set, units)}" }
    return joinDot(time, top)
}

/** Filtered row "Bench press  87.5 × 5 · 87.5 × 5 · 85 × 6 kg" (R7-8): unit once, at the end. */
@Composable
fun inlineSets(name: String, sets: List<WorkoutSet>, units: PreferredUnits): String {
    val weighted = sets.any { it.weightKg != null }
    val parts = sets.map { s ->
        val reps = s.reps?.toString()
        val w = s.weightKg?.let { UnitConverter.weightToDisplayUnit(it, units) }
        when {
            w != null && reps != null -> "$w × $reps"
            w != null -> w
            reps != null -> reps
            else -> s.durationSeconds?.let(::clock) ?: s.distanceMeters?.let { "${it.roundToInt()} m" } ?: ""
        }
    }.filter { it.isNotEmpty() }
    val unit = if (weighted) " " + UnitConverter.getWeightUnitLabel(units) else ""
    return stringResource(R.string.sessions_filtered_sets, name, parts.joinToString(" · ") + unit)
}

/** Chart dates "7 Sep" (R7-5), in the user's locale. */
fun dayMonth(millis: Long, zone: java.time.ZoneId, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "dMMM"), locale)
        .format(java.time.Instant.ofEpochMilli(millis).atZone(zone))

/** "October 2026" in the user's locale. */
fun monthTitle(month: YearMonth, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMMyyyy"), locale).format(month)

/** Y labels: whole numbers unless the step needs the half. */
fun tickText(value: Float): String =
    if (value == value.roundToInt().toFloat()) value.roundToInt().toString()
    else String.format(Locale.getDefault(), "%.1f", value)

/** y ticks for a chart, in the display unit. */
fun chartTicks(p: LiftProgress, units: PreferredUnits): List<Float> {
    val values = p.points.map { displayWeight(it.weightKg, units) }
    return ProgressStats.yTicks(values.min(), values.max())
}
