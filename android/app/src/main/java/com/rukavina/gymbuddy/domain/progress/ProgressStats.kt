package com.rukavina.gymbuddy.domain.progress

import com.rukavina.gymbuddy.domain.exercise.ExerciseHistory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PerformedExercise
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutSet
import com.rukavina.gymbuddy.domain.usecase.template.TemplateStats
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.floor

/** A lift that can go on the Progress chart (Figma R7-4: "12 sessions"). */
data class Lift(val exerciseId: String, val name: String, val sessionCount: Int)

/** One chart point: a session's heaviest set of the lift (R7-5). */
data class LiftPoint(val sessionId: String, val startedAt: Long, val set: WorkoutSet) {
    val weightKg: Float get() = set.weightKg ?: 0f
}

/**
 * The chart for one lift: [points] oldest first, inside the window.
 * [change] = last minus first point in kg; [weeks] between them.
 */
data class LiftProgress(val lift: Lift, val points: List<LiftPoint>, val weeks: Int) {
    val first: Float get() = points.first().weightKg
    val last: Float get() = points.last().weightKg
    val change: Float get() = last - first
}

/** "This week" line (R7-1): sessions, time, kg lifted (weight × reps). */
data class WeekTotals(val sessions: Int, val seconds: Long, val volumeKg: Float)

/** Month header in All sessions (R7-7): "October 2026 · 13 sessions · 41,200 kg". */
data class MonthGroup(val month: YearMonth, val sessions: List<WorkoutSession>, val volumeKg: Float)

/**
 * Pure rules for the Progress tab, All sessions and the exercise page's
 * Progress block (Plan 7 step 08). Computed from finished sessions, never
 * stored. Tombstoned sessions never count. Saved sessions hold only done
 * sets (06a), so every stored set counts.
 */
object ProgressStats {

    /** No card until a lift has this many sessions (Figma note 2)… */
    const val MIN_SESSIONS = 3

    /** …spread over at least this many days (2 weeks). */
    const val MIN_SPAN_DAYS = 14L

    /** Chart window: the 8 weeks up to the latest session (owner decision 5). */
    const val WINDOW_DAYS = 56L

    /** Recent list on Progress (R7-1). */
    const val RECENT = 3 // F15: 5 cards felt heavy

    private const val DAY_MS = 86_400_000L

    private val WEIGHT_TYPES = setOf(
        ExerciseTrackingType.WEIGHT_REPS,
        ExerciseTrackingType.WEIGHT_DURATION,
        ExerciseTrackingType.WEIGHT_DISTANCE,
    )

    private fun live(sessions: List<WorkoutSession>) = sessions.filter { it.deletedAt == null }

    /** A weighted type with a set that has weight. */
    private fun weighted(pe: PerformedExercise) = pe.exerciseTrackingType in WEIGHT_TYPES

    private fun heaviest(sets: List<WorkoutSet>): WorkoutSet? =
        sets.filter { (it.weightKg ?: 0f) > 0f }.maxWithOrNull(ExerciseHistory.SetOrder)

    /** Each session's heaviest set of [exerciseId], oldest first. */
    fun points(exerciseId: String, sessions: List<WorkoutSession>): List<LiftPoint> =
        live(sessions).mapNotNull { s ->
            val sets = s.performedExercises.filter { it.exerciseId == exerciseId && weighted(it) }.flatMap { it.sets }
            heaviest(sets)?.let { LiftPoint(s.id, s.startedAt, it) }
        }.sortedBy { it.startedAt }

    /**
     * Lifts you can chart (notes 2 and 4): weighted, 3+ sessions with a
     * weight over 2+ weeks. Most sessions first, then by name. The name is
     * the newest snapshot.
     */
    fun eligibleLifts(sessions: List<WorkoutSession>): List<Lift> {
        val byExercise = live(sessions)
            .flatMap { s -> s.performedExercises.filter(::weighted).map { s to it } }
            .groupBy { it.second.exerciseId }
        return byExercise.mapNotNull { (id, rows) ->
            val withWeight = rows.filter { (_, pe) -> heaviest(pe.sets) != null }
            val sessionsDone = withWeight.map { it.first }.distinctBy { it.id }
            if (sessionsDone.size < MIN_SESSIONS) return@mapNotNull null
            val span = sessionsDone.maxOf { it.startedAt } - sessionsDone.minOf { it.startedAt }
            if (span < MIN_SPAN_DAYS * DAY_MS) return@mapNotNull null
            val name = withWeight.maxBy { it.first.startedAt }.second.exerciseName
            Lift(id, name, sessionsDone.size)
        }.sortedWith(compareByDescending<Lift> { it.sessionCount }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    /** The stored lift if it is still eligible, else the most-logged one (decision 4). */
    fun chosenLift(lifts: List<Lift>, storedId: String?): Lift? =
        lifts.firstOrNull { it.exerciseId == storedId } ?: lifts.firstOrNull()

    /**
     * The lift's chart: points in the 8 weeks up to its latest session;
     * at least [MIN_SESSIONS] (older ones fill in when the window is thin).
     */
    fun progress(lift: Lift, sessions: List<WorkoutSession>): LiftProgress? {
        val all = points(lift.exerciseId, sessions)
        if (all.size < 2) return null
        val from = all.last().startedAt - WINDOW_DAYS * DAY_MS
        val inWindow = all.filter { it.startedAt >= from }
        val shown = if (inWindow.size >= MIN_SESSIONS) inWindow else all.takeLast(MIN_SESSIONS)
        val weeks = ((shown.last().startedAt - shown.first().startedAt) / (7 * DAY_MS)).toInt().coerceAtLeast(1)
        return LiftProgress(lift, shown, weeks)
    }

    /** Sum of weight × reps over every set (sets without either add 0). */
    fun volumeKg(session: WorkoutSession): Float =
        session.performedExercises.sumOf { pe ->
            pe.sets.sumOf { ((it.weightKg ?: 0f) * (it.reps ?: 0)).toDouble() }
        }.toFloat()

    /** This week, Monday first, by the day each session was done in [zone] (decision 6). */
    fun week(sessions: List<WorkoutSession>, today: LocalDate, zone: ZoneId): WeekTotals {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)
        val inWeek = live(sessions).filter { s ->
            val day = Instant.ofEpochMilli(TemplateStats.doneAt(s)).atZone(zone).toLocalDate()
            !day.isBefore(monday) && !day.isAfter(sunday)
        }
        return WeekTotals(inWeek.size, inWeek.sumOf { it.durationSeconds.toLong() }, inWeek.sumOf { volumeKg(it).toDouble() }.toFloat())
    }

    /** Live sessions, newest first. */
    fun newestFirst(sessions: List<WorkoutSession>): List<WorkoutSession> =
        live(sessions).sortedByDescending { it.startedAt }

    /**
     * Sessions with a PR (note 1): a weighted lift's heaviest set beats
     * every earlier session of that lift. A lift's first session is no PR.
     */
    fun prSessions(sessions: List<WorkoutSession>): Set<String> = prLifts(sessions).keys

    /** Session id → the lifts (exercise ids) that set a PR in it, same rule as [prSessions]. */
    fun prLifts(sessions: List<WorkoutSession>): Map<String, Set<String>> {
        val best = HashMap<String, WorkoutSet>()
        val prs = HashMap<String, MutableSet<String>>()
        for (s in live(sessions).sortedBy { it.startedAt }) {
            val tops = s.performedExercises.filter(::weighted).groupBy { it.exerciseId }
                .mapNotNull { (id, pes) -> heaviest(pes.flatMap { it.sets })?.let { id to it } }
            for ((id, top) in tops) {
                val before = best[id]
                if (before != null && ExerciseHistory.SetOrder.compare(top, before) > 0) prs.getOrPut(s.id) { HashSet() } += id
                if (before == null || ExerciseHistory.SetOrder.compare(top, before) > 0) best[id] = top
            }
        }
        return prs
    }

    /**
     * The set a session card shows ("Squat 110 kg × 5", R7-1): the
     * heaviest weighted set of the session, else the first exercise's best.
     */
    fun topSet(session: WorkoutSession): Pair<PerformedExercise, WorkoutSet>? {
        val weightedTop = session.performedExercises.filter(::weighted)
            .mapNotNull { pe -> heaviest(pe.sets)?.let { pe to it } }
            .maxWithOrNull { a, b -> ExerciseHistory.SetOrder.compare(a.second, b.second) }
        if (weightedTop != null) return weightedTop
        return session.performedExercises.sortedBy { it.orderIndex }.firstNotNullOfOrNull { pe ->
            pe.sets.maxWithOrNull(ExerciseHistory.SetOrder)?.let { pe to it }
        }
    }

    /** Exercises you have logged (All sessions filter sheet): most sessions first. */
    fun loggedExercises(sessions: List<WorkoutSession>): List<Lift> =
        live(sessions)
            .flatMap { s -> s.performedExercises.filter { it.sets.isNotEmpty() }.map { s to it } }
            .groupBy { it.second.exerciseId }
            .map { (id, rows) ->
                Lift(id, rows.maxBy { it.first.startedAt }.second.exerciseName, rows.map { it.first.id }.distinct().size)
            }
            .sortedWith(compareByDescending<Lift> { it.sessionCount }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    /** Newest month first, sessions newest first inside. */
    fun byMonth(sessions: List<WorkoutSession>, zone: ZoneId): List<MonthGroup> =
        newestFirst(sessions)
            .groupBy { YearMonth.from(Instant.ofEpochMilli(it.startedAt).atZone(zone)) }
            .map { (month, list) -> MonthGroup(month, list, list.sumOf { volumeKg(it).toDouble() }.toFloat()) }

    /** A session's sets of one exercise, in order (filtered row, R7-8). */
    fun setsOf(session: WorkoutSession, exerciseId: String): List<WorkoutSet> =
        session.performedExercises.filter { it.exerciseId == exerciseId }.sortedBy { it.orderIndex }
            .flatMap { pe -> pe.sets.sortedBy { it.orderIndex } }

    /**
     * Three round y ticks covering [min]..[max] (R7-5: 80, 85, 90), in the
     * display unit. Step from 1, 2, 2.5, 5 × 10ⁿ.
     */
    fun yTicks(min: Float, max: Float): List<Float> {
        val span = (max - min).coerceAtLeast(0f)
        var step = niceSteps().first { it * 2 >= span && it > 0f }
        while (true) {
            val low = floor(min / step) * step
            if (low + 2 * step >= max) return listOf(low, low + step, low + 2 * step)
            step = niceSteps().first { it > step }
        }
    }

    private fun niceSteps(): Sequence<Float> = generateSequence(1f) { it * 10f }
        .flatMap { base -> sequenceOf(base, 2 * base, 2.5f * base, 5 * base) }
        .let { up -> sequenceOf(0.5f) + up }

    /** Drag on the chart (R7-6): the nearest point index for [x] in [width] (points evenly spaced). */
    fun nearestIndex(x: Float, width: Float, count: Int): Int {
        if (count <= 1 || width <= 0f) return 0
        val step = width / (count - 1)
        return (x / step).let { kotlin.math.round(it).toInt() }.coerceIn(0, count - 1)
    }

}
