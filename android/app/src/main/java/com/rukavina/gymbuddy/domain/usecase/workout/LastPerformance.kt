package com.rukavina.gymbuddy.domain.usecase.workout

import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutSet

/**
 * F18 (Plan 7 11n): new sets start from the last time you did the lift,
 * not from 0 kg. Pure; the workout screen formats the values.
 */
object LastPerformance {

    /**
     * The sets of [exerciseId] in the newest saved session that has any,
     * in set order. A saved session holds only logged sets, so
     * isCompleted isn't checked (older data may lack it); sets with no
     * values are skipped. Deleted sessions are ignored. Null when the
     * exercise was never logged.
     */
    fun lastSets(sessions: List<WorkoutSession>, exerciseId: String): List<WorkoutSet>? =
        sessions.asSequence()
            .filter { it.deletedAt == null }
            .sortedByDescending { it.startedAt }
            .mapNotNull { session ->
                session.performedExercises
                    .sortedBy { it.orderIndex }
                    .firstOrNull { pe -> pe.exerciseId == exerciseId && pe.sets.any(::isLogged) }
                    ?.sets?.filter(::isLogged)?.sortedBy { it.orderIndex }
            }
            .firstOrNull()

    /**
     * [count] starting sets from [last]: set by set, and past its end the
     * last one repeats. No [last] gives nulls (keep the planned values).
     */
    fun prefill(last: List<WorkoutSet>?, count: Int): List<WorkoutSet?> =
        (0 until count.coerceAtLeast(0)).map { i -> last?.getOrNull(i) ?: last?.lastOrNull() }

    private fun isLogged(set: WorkoutSet): Boolean =
        (set.weightKg != null || set.reps != null || set.durationSeconds != null || set.distanceMeters != null)
}
