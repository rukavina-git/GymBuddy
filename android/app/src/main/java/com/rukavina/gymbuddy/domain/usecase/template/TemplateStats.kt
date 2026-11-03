package com.rukavina.gymbuddy.domain.usecase.template

import com.rukavina.gymbuddy.domain.model.StarterPlan
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

/**
 * Pure rules for Home, the Workouts tab and the template page (Plan 7
 * step 05a). Computed from sessions, never stored. Tombstoned sessions
 * never count.
 */
object TemplateStats {

    /** When a session counts as done: its end, or its start if it has none. */
    fun doneAt(session: WorkoutSession): Long = session.endedAt ?: session.startedAt

    /** Template id → when it was last done. Templates never done are absent. */
    fun lastDone(sessions: List<WorkoutSession>): Map<String, Long> =
        sessions.asSequence()
            .filter { it.deletedAt == null && it.templateId != null }
            .groupBy { it.templateId!! }
            .mapValues { (_, s) -> s.maxOf(::doneAt) }

    /**
     * "about N min" (Figma flag, owner): the average length of this
     * template's finished sessions, rounded to 5 min (at least 5). Null
     * until there is one finished session with a length.
     */
    fun averageMinutes(templateId: String, sessions: List<WorkoutSession>): Int? {
        val lengths = sessions
            .filter { it.deletedAt == null && it.templateId == templateId && it.endedAt != null && it.durationSeconds > 0 }
            .map { it.durationSeconds }
        if (lengths.isEmpty()) return null
        val minutes = lengths.average() / 60.0
        return ((minutes / 5.0).roundToInt() * 5).coerceAtLeast(5)
    }

    /**
     * Templates by recency: newest last session first; never-done ones
     * after, by title. [lastDone] comes from [lastDone].
     */
    fun byRecency(templates: List<WorkoutTemplate>, lastDone: Map<String, Long>): List<WorkoutTemplate> =
        templates.sortedWith(
            compareByDescending<WorkoutTemplate> { lastDone[it.id] ?: Long.MIN_VALUE }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        )

    /**
     * Home rows (council 2026-11-01): your 3 most recently done
     * templates. With none done yet, the active plan's templates in plan
     * order (R11-2). Empty when there is neither.
     */
    fun homeTemplates(
        templates: List<WorkoutTemplate>,
        sessions: List<WorkoutSession>,
        activePlan: StarterPlan?,
        count: Int = 3,
    ): List<WorkoutTemplate> {
        val done = lastDone(sessions)
        val recent = templates.filter { it.id in done }
        if (recent.isNotEmpty()) return byRecency(recent, done).take(count)
        val byId = templates.associateBy { it.id }
        return activePlan?.templateIds.orEmpty().mapNotNull(byId::get).take(count)
    }

    /** One week, Monday first (Figma strip M…S). */
    data class Week(
        val monday: LocalDate,
        val today: LocalDate,
        /** Days with at least one session, inside this week. */
        val doneDays: Set<LocalDate>,
        val sessionCount: Int,
        val totalSeconds: Long,
    )

    /** This week's sessions, by the day each was done in [zone]. */
    fun week(sessions: List<WorkoutSession>, today: LocalDate, zone: ZoneId): Week {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)
        val inWeek = sessions
            .filter { it.deletedAt == null }
            .map { it to Instant.ofEpochMilli(doneAt(it)).atZone(zone).toLocalDate() }
            .filter { (_, day) -> !day.isBefore(monday) && !day.isAfter(sunday) }
        return Week(
            monday = monday,
            today = today,
            doneDays = inWeek.map { it.second }.toSet(),
            sessionCount = inWeek.size,
            totalSeconds = inWeek.sumOf { it.first.durationSeconds.toLong() },
        )
    }

    /** Where [templateId] sits in a live plan: (plan, 1-based day). First plan wins. */
    fun planDay(templateId: String, plans: List<StarterPlan>): Pair<StarterPlan, Int>? =
        plans.asSequence()
            .filter { !it.deprecated }
            .mapNotNull { plan -> plan.templateIds.indexOf(templateId).takeIf { it >= 0 }?.let { plan to it + 1 } }
            .firstOrNull()

    /**
     * First free name for a copy. [name] makes the n-th candidate
     * (1 → "Push A (copy)", 2 → "Push A (copy 2)"; text from resources).
     */
    fun copyTitle(taken: Collection<String>, name: (Int) -> String): String =
        generateSequence(1) { it + 1 }.map(name).first { it !in taken }
}
