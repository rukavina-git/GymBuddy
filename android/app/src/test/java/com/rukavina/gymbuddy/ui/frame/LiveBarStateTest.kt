package com.rukavina.gymbuddy.ui.frame

import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.ui.workout.ActiveExercise
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutUiState
import com.rukavina.gymbuddy.ui.workout.RestState
import com.rukavina.gymbuddy.ui.workout.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveBarStateTest {

    private fun exercise(name: String, vararg done: Boolean) = ActiveExercise(
        id = name, exerciseId = name, exerciseName = name,
        exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
        sets = done.mapIndexed { i, d -> WorkoutSet(id = "$name$i", setNumber = i + 1, isCompleted = d) },
        plannedSets = done.size, plannedReps = null, restSeconds = null,
    )

    private fun state(vararg exercises: ActiveExercise) = ActiveWorkoutUiState(
        workoutTitle = "Push A", elapsedSeconds = 2292, exercises = exercises.toList(),
    )

    @Test
    fun noWorkoutMeansNoBar() {
        assertNull(ActiveWorkoutUiState().toLiveBarState())
        val running = state(exercise("Bench press", false))
        assertNull(running.copy(workoutSaved = true).toLiveBarState())
        assertNull(running.copy(workoutDiscarded = true).toLiveBarState())
    }

    @Test
    fun showsFirstExerciseWithASetLeft() {
        val bar = state(
            exercise("Squat", true, true),
            exercise("Bench press", true, true, false, false),
        ).toLiveBarState()!!

        assertEquals("Push A · Bench press", bar.title)
        assertEquals(3, bar.setNumber)
        assertEquals(4, bar.setCount)
        assertFalse(bar.allDone)
        assertEquals(2292, bar.elapsedSeconds)
        assertEquals(4f / 6f, bar.progress, 0.0001f)
    }

    @Test
    fun allDoneFallsBackToLastExercise() {
        val bar = state(exercise("Squat", true), exercise("Row", true, true)).toLiveBarState()!!

        assertEquals("Push A · Row", bar.title)
        assertNull(bar.setNumber)
        assertTrue(bar.allDone)
        assertEquals(1f, bar.progress)
    }

    @Test
    fun exercisesWithoutSetsGiveZeroProgressAndNoSet() {
        val bar = state(exercise("Plank")).copy(workoutTitle = "").toLiveBarState()!!

        assertEquals("Plank", bar.title)
        assertNull(bar.setNumber)
        assertFalse(bar.allDone)
        assertEquals(0f, bar.progress)
    }

    @Test
    fun elapsedIsMinutesThenHours() {
        assertEquals("0:00", formatElapsed(-5))
        assertEquals("38:12", formatElapsed(2292))
        assertEquals("1:02:03", formatElapsed(3723))
    }

    @Test
    fun emptyWorkoutShowsTitleAndClockOnly() {
        val bar = ActiveWorkoutUiState(workoutTitle = "Workout", workoutStartTime = 1L, elapsedSeconds = 30).toLiveBarState()!!
        assertEquals("Workout", bar.title)
        assertNull(bar.setNumber)
        assertFalse(bar.allDone)
        assertNull(bar.rest)
    }

    @Test
    fun restCountsDownThenPasses() {
        val running = state(exercise("Bench press", true, true, true, false))
            .copy(rest = RestState(afterSetId = "Bench press2", startedAtElapsed = 2292 - 48, seconds = 120))
        assertEquals(LiveBarRest(setNumber = 3, setCount = 4, leftSeconds = 72), running.toLiveBarState()!!.rest)

        val passed = running.copy(elapsedSeconds = 2292 + 117)
        assertEquals(-45L, passed.toLiveBarState()!!.rest!!.leftSeconds)
    }

    @Test
    fun noRestOnceEverySetIsDone() {
        val done = state(exercise("Bench press", true, true))
            .copy(rest = RestState(afterSetId = "Bench press1", startedAtElapsed = 2290, seconds = 120))
        assertNull(done.toLiveBarState()!!.rest)
    }
}
