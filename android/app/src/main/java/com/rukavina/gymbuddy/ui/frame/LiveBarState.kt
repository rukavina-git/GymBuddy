package com.rukavina.gymbuddy.ui.frame

import com.rukavina.gymbuddy.ui.workout.ActiveExercise
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutUiState
import com.rukavina.gymbuddy.ui.workout.RestState

/**
 * What the live bar shows while a workout runs (Figma 85:10033, "On a
 * set" and "Resting").
 *
 * [setNumber] is the next set to do in the current exercise, null once
 * every set is done (or when there are no exercises yet). [progress] is
 * done sets over all sets, 0..1. [rest] replaces the set line while a
 * rest runs and sets are left.
 */
data class LiveBarState(
    val title: String,
    val setNumber: Int?,
    val setCount: Int,
    val allDone: Boolean,
    val elapsedSeconds: Long,
    val progress: Float,
    val rest: LiveBarRest? = null,
)

/**
 * "Set 3 of 4 done · rest 1:12" counting down; [leftSeconds] below 0 is
 * "rest +0:45" (no colour change, no buzz: Figma 85:10033).
 */
data class LiveBarRest(val setNumber: Int, val setCount: Int, val leftSeconds: Long)

/**
 * Null when no workout runs (no exercises, or saved / discarded). The
 * exercise is the one on screen while it has a set left, else the first
 * one with a set left; when all are done, the last one.
 */
fun ActiveWorkoutUiState.toLiveBarState(): LiveBarState? {
    // Unfinished (R17-E2): Home's card asks first; no live bar until Resume.
    if (!running || unfinishedSince != null) return null
    if (exercises.isEmpty()) {
        return LiveBarState(workoutTitle, setNumber = null, setCount = 0, allDone = false, elapsedSeconds = elapsedSeconds, progress = 0f)
    }
    val current = liveExercise() ?: return null
    val next = current.sets.indexOfFirst { !it.isCompleted }.takeIf { it >= 0 }
    val total = exercises.sumOf { it.sets.size }
    val done = exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }
    return LiveBarState(
        title = listOf(workoutTitle, current.exerciseName).filter { it.isNotBlank() }.joinToString(" · "),
        setNumber = next?.plus(1),
        setCount = current.sets.size,
        allDone = total > 0 && done == total,
        elapsedSeconds = elapsedSeconds,
        progress = if (total == 0) 0f else done.toFloat() / total,
        rest = liveBarRest(),
    )
}

/**
 * The exercise the live bar and the notification show: the one on screen
 * while it has a set left, else the first with a set left; when all are
 * done, the last one. Null without exercises.
 */
internal fun ActiveWorkoutUiState.liveExercise(): ActiveExercise? {
    if (exercises.isEmpty()) return null
    return currentExercise?.takeIf { !it.allDone }
        ?: exercises.firstOrNull { !it.allDone }
        ?: exercises.last()
}

/** The rest that's running: its set still exists and sets are left. */
internal fun ActiveWorkoutUiState.liveRest(): RestState? {
    val rest = rest ?: return null
    if (setsLeft == 0) return null
    return rest.takeIf { exercises.any { ex -> ex.sets.any { it.id == rest.afterSetId } } }
}

private fun ActiveWorkoutUiState.liveBarRest(): LiveBarRest? {
    val rest = liveRest() ?: return null
    val exercise = exercises.first { ex -> ex.sets.any { it.id == rest.afterSetId } }
    val set = exercise.sets.first { it.id == rest.afterSetId }
    return LiveBarRest(set.setNumber, exercise.sets.size, rest.seconds - (elapsedSeconds - rest.startedAtElapsed))
}

/** 38:12, or 1:02:03 from an hour on. */
fun formatElapsed(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
