package com.rukavina.gymbuddy.domain.exercise

import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutSet

/** One session's best set for an exercise (exercise page History, R13-1). */
data class ExerciseHistoryEntry(val sessionId: String, val startedAt: Long, val best: WorkoutSet)

/**
 * Your numbers for one exercise from finished sessions: [entries] newest
 * first (one per session, its best set), [last] = the newest session's
 * best set, [best] = the best set ever. Empty when never done (R13-8).
 */
data class ExerciseHistory(val entries: List<ExerciseHistoryEntry>, val best: WorkoutSet?) {
    val last: WorkoutSet? get() = entries.firstOrNull()?.best
    val sessionCount: Int get() = entries.size

    companion object {
        val Empty = ExerciseHistory(emptyList(), null)

        /** Heaviest, then most reps, longest, farthest (same order as the workout summary). */
        val SetOrder: Comparator<WorkoutSet> = compareBy<WorkoutSet> { it.weightKg ?: 0f }
            .thenBy { it.reps ?: 0 }
            .thenBy { it.durationSeconds ?: 0 }
            .thenBy { it.distanceMeters ?: 0f }

        fun of(exerciseId: String, sessions: List<WorkoutSession>): ExerciseHistory {
            val entries = sessions
                .asSequence()
                .filter { it.deletedAt == null }
                .mapNotNull { session ->
                    val sets = session.performedExercises.filter { it.exerciseId == exerciseId }.flatMap { it.sets }
                    sets.maxWithOrNull(SetOrder)?.let { ExerciseHistoryEntry(session.id, session.startedAt, it) }
                }
                .sortedByDescending { it.startedAt }
                .toList()
            return ExerciseHistory(entries, entries.map { it.best }.maxWithOrNull(SetOrder))
        }
    }
}
