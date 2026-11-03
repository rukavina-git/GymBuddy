package com.rukavina.gymbuddy.ui.template

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.TemplateExercise
import com.rukavina.gymbuddy.utils.UnitConverter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Text for template lists and the template page (Plan 7 05a). */

@Composable
fun joinDot(vararg parts: String?): String {
    val res = LocalContext.current.resources
    return parts.filterNotNull().filter { it.isNotEmpty() }.reduceOrNull { a, b -> res.getString(R.string.sep_dot, a, b) } ?: ""
}

@Composable
fun exerciseCountText(count: Int): String = pluralStringResource(R.plurals.exercise_count, count, count)

@Composable
fun aboutText(minutes: Int?): String? = minutes?.let { stringResource(R.string.about_minutes, it) }

/** "today", "yesterday", "3 days ago", then a short date (Figma R11-1). */
@Composable
fun relativeDay(millis: Long, today: LocalDate, zone: ZoneId): String {
    val day = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(day, today).toInt()
    return when {
        days <= 0 -> stringResource(R.string.done_today)
        days == 1 -> stringResource(R.string.done_yesterday)
        days < 7 -> pluralStringResource(R.plurals.done_days_ago, days, days)
        else -> shortDate(millis, zone)
    }
}

/** "Tue 27 Oct" in the user's locale (Workouts tab right column). */
fun shortDate(millis: Long, zone: ZoneId, locale: Locale = Locale.getDefault()): String {
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEdMMM")
    return DateTimeFormatter.ofPattern(pattern, locale).format(Instant.ofEpochMilli(millis).atZone(zone))
}

/** m:ss (rest, timed sets). */
fun clock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

/** Week total: "2 h 41 m" or "41 m". */
@Composable
fun hoursMinutes(totalSeconds: Long): String {
    val minutes = (totalSeconds / 60).toInt()
    return if (minutes >= 60) stringResource(R.string.week_time_hm, minutes / 60, minutes % 60)
    else stringResource(R.string.week_time_m, minutes)
}

/** Template page row: "4 × 6 · 80 kg", "3 × 1:00", "3 × 400 m" (Figma Page A). */
@Composable
fun targetText(exercise: TemplateExercise, units: PreferredUnits): String {
    val each = when (exercise.exerciseTrackingType) {
        ExerciseTrackingType.WEIGHT_REPS, ExerciseTrackingType.REPS_ONLY -> exercise.plannedReps?.toString()
        ExerciseTrackingType.DURATION, ExerciseTrackingType.WEIGHT_DURATION -> exercise.plannedDurationSeconds?.let(::clock)
        ExerciseTrackingType.DISTANCE_DURATION, ExerciseTrackingType.WEIGHT_DISTANCE ->
            exercise.plannedDistanceMeters?.let { stringResource(R.string.distance_m, it.toInt()) }
    }
    val sets = if (each != null) stringResource(R.string.target_sets, exercise.plannedSets, each)
    else pluralStringResource(R.plurals.set_count, exercise.plannedSets, exercise.plannedSets)
    val weight = exercise.plannedWeightKg
        ?.takeIf { exercise.exerciseTrackingType.name.startsWith("WEIGHT") }
        ?.let { stringResource(R.string.weight_value, UnitConverter.weightToDisplayUnit(it, units), UnitConverter.getWeightUnitLabel(units)) }
    return joinDot(sets, weight)
}
