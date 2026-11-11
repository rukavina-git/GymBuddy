package com.rukavina.gymbuddy.domain.exercise

import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PerformedExercise
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseHistoryTest {

    private fun set(kg: Float?, reps: Int?, seconds: Int? = null) =
        WorkoutSet(id = "s$kg-$reps-$seconds", weightKg = kg, reps = reps, durationSeconds = seconds, isCompleted = true, orderIndex = 0)

    private fun session(id: String, at: Long, exerciseId: String, vararg sets: WorkoutSet, deletedAt: Long? = null) = WorkoutSession(
        id = id, startedAt = at, endedAt = at + 1, durationSeconds = 1, title = id, deletedAt = deletedAt,
        performedExercises = listOf(
            PerformedExercise(
                id = "$id-pe", exerciseId = exerciseId, orderIndex = 0, exerciseName = "", exerciseCategory = ExerciseCategory.STRENGTH,
                exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS, exercisePrimaryMuscles = emptyList(), sets = sets.toList(),
            ),
        ),
    )

    @Test
    fun newestFirstWithEachSessionsBestSetAndBestEver() {
        val h = ExerciseHistory.of(
            "bench",
            listOf(
                session("old", 1, "bench", set(90f, 3), set(85f, 5)),
                session("new", 3, "bench", set(87.5f, 5), set(87.5f, 3)),
                session("mid", 2, "bench", set(85f, 5)),
                session("other", 4, "row", set(100f, 5)),
                session("gone", 5, "bench", set(200f, 1), deletedAt = 9),
            ),
        )
        assertEquals(listOf("new", "mid", "old"), h.entries.map { it.sessionId })
        assertEquals(set(87.5f, 5), h.last)
        assertEquals(set(90f, 3), h.best)
        assertEquals(3, h.sessionCount)
    }

    @Test
    fun noWeightComparesRepsThenTime() {
        val h = ExerciseHistory.of("dip", listOf(session("a", 1, "dip", set(null, 8), set(null, 12)), session("b", 2, "dip", set(null, 10))))
        assertEquals(set(null, 12), h.best)
        assertEquals(set(null, 10), h.last)
    }

    @Test
    fun neverDoneIsEmpty() {
        val h = ExerciseHistory.of("bench", listOf(session("a", 1, "row", set(100f, 5))))
        assertTrue(h.entries.isEmpty())
        assertNull(h.last)
        assertNull(h.best)
    }
}
