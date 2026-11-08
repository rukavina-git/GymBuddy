package com.rukavina.gymbuddy.domain.usecase.workout

import com.rukavina.gymbuddy.domain.usecase.workout.StaleWorkout.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StaleWorkoutTest {
    private val hour = 60 * 60 * 1000L

    @Test
    fun lastActivityIsTheLatestSetOrResumeElseTheStart() {
        assertEquals(100L, StaleWorkout.lastActivity(100L, emptyList(), null))
        assertEquals(500L, StaleWorkout.lastActivity(100L, listOf(300L, 500L), null))
        assertEquals(700L, StaleWorkout.lastActivity(100L, listOf(300L), resumedAt = 700L))
    }

    @Test
    fun staleAtTwoHoursExpiredAtTwentyFour() {
        assertEquals(Status.Active, StaleWorkout.status(0L, 2 * hour - 1))
        assertEquals(Status.Stale, StaleWorkout.status(0L, 2 * hour))
        assertEquals(Status.Stale, StaleWorkout.status(0L, 24 * hour - 1))
        assertEquals(Status.Expired, StaleWorkout.status(0L, 24 * hour))
    }

    @Test
    fun finishAtLastSetUsesTheLatestSetsTimeAndClock() {
        assertNull(StaleWorkout.endAtLastSet(emptyList()))
        val end = StaleWorkout.endAtLastSet(listOf(1_000L to 60L, 5_000L to 1_800L, 3_000L to 900L))
        assertEquals(StaleWorkout.End(5_000L, 1_800L), end)
    }
}
