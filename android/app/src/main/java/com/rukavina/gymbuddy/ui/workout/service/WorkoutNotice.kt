package com.rukavina.gymbuddy.ui.workout.service

import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.ui.frame.liveExercise
import com.rukavina.gymbuddy.ui.frame.liveRest
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutUiState
import com.rukavina.gymbuddy.ui.workout.RestState
import com.rukavina.gymbuddy.ui.workout.WorkoutSet

/**
 * What the ongoing workout notification shows (Plan 7 06c, Figma 53:3470),
 * without the ticking clock: the system chronometer counts from
 * [counterBase], so the notification isn't posted every second.
 *
 * The exercise is the live bar's one ([liveExercise]); [set] is its next
 * set, null once every set is done. [resting]: a rest runs and sets are
 * left (the counter is then the rest, else the workout clock).
 */
data class WorkoutNotice(
    val workoutTitle: String,
    val exerciseName: String?,
    val trackingType: ExerciseTrackingType?,
    val set: WorkoutSet?,
    val setCount: Int,
    val units: PreferredUnits,
    val resting: Boolean,
    val paused: Boolean,
    /** F25: no set for 2 h; shows "Workout still running? Tap to finish". */
    val stale: Boolean = false,
    /** Wall-clock ms at which the counter read 0:00; null while paused. */
    val counterBase: Long? = null,
)

/** Starts, updates and stops the workout foreground service. */
interface WorkoutServiceStarter {
    /** Null stops the service (no workout runs). */
    fun update(notice: WorkoutNotice?)
}

/** The opt-in rest buzz (motion.md "Rest buzz"): one buzz when the rest is over. */
interface RestBuzzScheduler {
    /** Buzz once in [leftSeconds]; replaces any buzz already scheduled. */
    fun schedule(leftSeconds: Long)
    fun cancel()
}

/** Null when no workout runs. [WorkoutNotice.counterBase] is left for the caller. */
fun ActiveWorkoutUiState.toNotice(): WorkoutNotice? {
    if (!running) return null
    val exercise = liveExercise()
    return WorkoutNotice(
        workoutTitle = workoutTitle,
        exerciseName = exercise?.exerciseName,
        trackingType = exercise?.exerciseTrackingType,
        set = exercise?.currentSet,
        setCount = exercise?.sets?.size ?: 0,
        units = preferredUnits,
        resting = liveRest() != null,
        paused = !isTimerRunning,
        // Unfinished (R17-E2, F25): paused until Resume; the notification asks to finish.
        stale = unfinishedSince != null,
    )
}

/** Seconds the notification's counter has run: the rest while resting, else the workout. */
fun ActiveWorkoutUiState.counterSeconds(): Long =
    liveRest()?.let { elapsedSeconds - it.startedAtElapsed } ?: elapsedSeconds

/**
 * The rest the buzz waits for: only with the buzz on, the clock running
 * and sets left. A new rest (or a changed length) is a new target.
 */
fun ActiveWorkoutUiState.buzzTarget(): RestState? =
    if (restBuzz && running && isTimerRunning) liveRest() else null
