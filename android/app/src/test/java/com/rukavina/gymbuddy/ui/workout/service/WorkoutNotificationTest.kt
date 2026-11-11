package com.rukavina.gymbuddy.ui.workout.service

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.media.AudioManager
import androidx.core.app.NotificationCompat
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.ui.workout.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Notification text (Figma 53:3470), chronometer and the buzz's phone rules (Plan 7 06c). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class WorkoutNotificationTest {

    private val context get() = RuntimeEnvironment.getApplication()

    private val bench = WorkoutNotice(
        workoutTitle = "Push A",
        exerciseName = "Bench Press",
        trackingType = ExerciseTrackingType.WEIGHT_REPS,
        set = WorkoutSet(id = "s4", setNumber = 4, weight = "82.5", reps = "8"),
        setCount = 4,
        units = PreferredUnits.METRIC,
        resting = false,
        paused = false,
        counterBase = 1_000L,
    )

    private fun text(notice: WorkoutNotice) = workoutNotificationText(context.resources, notice)

    @Test
    fun `a set shows exercise, set number and values under the workout title`() {
        assertEquals(WorkoutNotificationText("Bench Press · set 4 of 4", "82.5 kg × 8", "Push A"), text(bench))
    }

    @Test
    fun `resting and paused say so in the header`() {
        assertEquals("Push A · rest", text(bench.copy(resting = true)).subText)
        assertEquals("Push A · paused", text(bench.copy(resting = true, paused = true)).subText)
    }

    @Test
    fun `timed sets, all done and no exercises`() {
        val plank = bench.copy(
            exerciseName = "Plank",
            trackingType = ExerciseTrackingType.DURATION,
            set = WorkoutSet(id = "p1", setNumber = 1, duration = "90"),
            setCount = 3,
        )
        assertEquals("1:30", text(plank).text)
        assertEquals(WorkoutNotificationText("Bench Press", "All sets done", "Push A"), text(bench.copy(set = null)))
        val empty = bench.copy(workoutTitle = "", exerciseName = null, trackingType = null, set = null, setCount = 0)
        assertEquals(WorkoutNotificationText("Workout", "No exercises yet", "Workout"), text(empty))
    }

    @Test
    fun `the chronometer counts from the base and stops while paused`() {
        val running = WorkoutNotification.build(context, bench)
        assertTrue(running.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertEquals(1_000L, running.`when`)
        assertTrue(running.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals("Open", running.actions.single().title)
        assertEquals(NotificationCompat.CATEGORY_STOPWATCH, running.category)

        val paused = WorkoutNotification.build(context, bench.copy(paused = true, counterBase = null))
        assertFalse(paused.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
    }

    @Test
    fun `the buzz follows the phone, silent and do not disturb stay quiet`() {
        val all = NotificationManager.INTERRUPTION_FILTER_ALL
        assertTrue(RestBuzz.allowed(AudioManager.RINGER_MODE_NORMAL, all))
        assertTrue(RestBuzz.allowed(AudioManager.RINGER_MODE_VIBRATE, all))
        assertFalse(RestBuzz.allowed(AudioManager.RINGER_MODE_SILENT, all))
        assertFalse(RestBuzz.allowed(AudioManager.RINGER_MODE_NORMAL, NotificationManager.INTERRUPTION_FILTER_PRIORITY))
        assertFalse(RestBuzz.allowed(AudioManager.RINGER_MODE_NORMAL, NotificationManager.INTERRUPTION_FILTER_NONE))
    }
}
