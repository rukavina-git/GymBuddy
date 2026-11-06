package com.rukavina.gymbuddy.ui.exercises.page

import com.rukavina.gymbuddy.domain.exercise.ExerciseHistory
import com.rukavina.gymbuddy.domain.exercise.ExerciseHistoryEntry
import com.rukavina.gymbuddy.domain.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Test

/** R17-D3: done once → "First time", Last / Best from two sessions. */
class FirstTimeRuleTest {

    private fun state(sessions: Int): ExercisePageUiState {
        val entries = (1..sessions).map { ExerciseHistoryEntry("s$it", it * 1000L, WorkoutSet(id = "x$it", weightKg = 80f, reps = 8, orderIndex = 0)) }
        return ExercisePageUiState(loaded = true, history = ExerciseHistory(entries, entries.firstOrNull()?.best))
    }

    @Test
    fun firstTimeOnlyForOneSession() {
        assertEquals(false, state(0).firstTime)
        assertEquals(true, state(1).firstTime)
        assertEquals(false, state(2).firstTime)
        assertEquals(false, state(3).firstTime)
    }
}
