package com.rukavina.gymbuddy.domain.progress

import com.rukavina.gymbuddy.domain.exercise.ExerciseHistory
import com.rukavina.gymbuddy.domain.model.PerformedExercise
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutSet

/**
 * Session detail tiles (Figma R14-1 / R14-6): they fit the data.
 * Weights → time · sets · lifted; a run (distance, no weight) →
 * time · distance · pace; anything else → time · sets.
 */
sealed interface SessionTiles {
    val seconds: Int

    data class Lifted(override val seconds: Int, val sets: Int, val liftedKg: Float) : SessionTiles

    /** [secondsPerKm] is null when no distance set has a time. */
    data class Distance(override val seconds: Int, val meters: Float, val secondsPerKm: Float?) : SessionTiles

    data class Plain(override val seconds: Int, val sets: Int) : SessionTiles
}

/** One exercise card (R14-1): sets in order, its top set (row line), PR pill. */
data class DetailExercise(
    val performed: PerformedExercise,
    val sets: List<WorkoutSet>,
    val top: WorkoutSet?,
    val isPr: Boolean,
)

/** Pure rules for the session detail screen (Plan 7 step 09a). */
object SessionDetail {

    fun tiles(session: WorkoutSession): SessionTiles {
        val sets = session.performedExercises.flatMap { it.sets }
        val seconds = session.durationSeconds
        if (sets.any { (it.weightKg ?: 0f) > 0f }) {
            return SessionTiles.Lifted(seconds, sets.size, ProgressStats.volumeKg(session))
        }
        val distanceSets = sets.filter { (it.distanceMeters ?: 0f) > 0f }
        if (distanceSets.isNotEmpty()) {
            val meters = distanceSets.sumOf { it.distanceMeters!!.toDouble() }.toFloat()
            val timed = distanceSets.filter { (it.durationSeconds ?: 0) > 0 }
            val timedMeters = timed.sumOf { it.distanceMeters!!.toDouble() }
            val pace = if (timed.isEmpty()) null else (timed.sumOf { it.durationSeconds!! } / (timedMeters / 1000.0)).toFloat()
            return SessionTiles.Distance(seconds, meters, pace)
        }
        return SessionTiles.Plain(seconds, sets.size)
    }

    /**
     * Cards in workout order. Top = heaviest set, then most reps, longest,
     * farthest ([ExerciseHistory.SetOrder]). PR = this lift's PR in this
     * session, judged on [all] (Progress rule).
     */
    fun exercises(session: WorkoutSession, all: List<WorkoutSession>): List<DetailExercise> {
        val prs = ProgressStats.prLifts(all)[session.id].orEmpty()
        return session.performedExercises.sortedBy { it.orderIndex }.map { pe ->
            val sets = pe.sets.sortedBy { it.orderIndex }
            DetailExercise(pe, sets, sets.maxWithOrNull(ExerciseHistory.SetOrder), pe.exerciseId in prs)
        }
    }
}
