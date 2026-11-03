package com.rukavina.gymbuddy.ui.frame

import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutUiState

/**
 * What the live bar shows while a workout runs (Figma 85:10033, "On a
 * set"). Resting and timed variants come with 7.6.
 *
 * [setNumber] is the next set to do in the current exercise, null once
 * every set is done. [progress] is done sets over all sets, 0..1.
 */
data class LiveBarState(
    val title: String,
    val setNumber: Int?,
    val setCount: Int,
    val allDone: Boolean,
    val elapsedSeconds: Long,
    val progress: Float,
)

/**
 * Null when no workout runs (no exercises, or saved / discarded). The
 * current exercise is the first one with a set left; when all are done,
 * the last one.
 */
fun ActiveWorkoutUiState.toLiveBarState(): LiveBarState? {
    if (exercises.isEmpty() || workoutSaved || workoutDiscarded) return null
    val current = exercises.firstOrNull { ex -> ex.sets.any { !it.isCompleted } } ?: exercises.last()
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
    )
}

/** 38:12, or 1:02:03 from an hour on. */
fun formatElapsed(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
