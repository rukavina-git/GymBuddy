package com.rukavina.gymbuddy.domain.usecase.workout

import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PerformedExercise
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LastPerformanceTest {

    private fun set(kg: Float?, reps: Int?, order: Int) =
        WorkoutSet(id = "s$order-$kg", weightKg = kg, reps = reps, isCompleted = true, orderIndex = order)

    private fun session(id: String, startedAt: Long, vararg exercises: Pair<String, List<WorkoutSet>>, deletedAt: Long? = null) =
        WorkoutSession(
            id = id,
            startedAt = startedAt,
            durationSeconds = 60,
            title = id,
            performedExercises = exercises.mapIndexed { i, (exId, sets) ->
                PerformedExercise(
                    id = "$id-pe$i",
                    exerciseId = exId,
                    orderIndex = i,
                    exerciseName = exId,
                    exerciseCategory = ExerciseCategory.STRENGTH,
                    exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
                    exercisePrimaryMuscles = emptyList(),
                    sets = sets,
                )
            },
            deletedAt = deletedAt,
        )

    @Test
    fun `newest session with the exercise wins, sets in order`() {
        val old = session("old", 1_000, "bench" to listOf(set(60f, 10, 0)))
        val new = session("new", 2_000, "bench" to listOf(set(82.5f, 6, 1), set(80f, 8, 0)))
        val other = session("other", 3_000, "squat" to listOf(set(100f, 5, 0)))
        val sets = LastPerformance.lastSets(listOf(old, other, new), "bench")!!
        assertEquals(listOf(80f, 82.5f), sets.map { it.weightKg })
    }

    @Test
    fun `deleted sessions and empty sets are skipped`() {
        val kept = session("kept", 1_000, "bench" to listOf(set(70f, 8, 0)))
        val deleted = session("del", 3_000, "bench" to listOf(set(90f, 8, 0)), deletedAt = 4_000)
        val empty = session("empty", 2_000, "bench" to listOf(set(null, null, 0)))
        val sets = LastPerformance.lastSets(listOf(deleted, empty, kept), "bench")!!
        assertEquals(listOf(70f), sets.map { it.weightKg })
    }

    @Test
    fun `never logged gives null`() {
        assertNull(LastPerformance.lastSets(listOf(session("a", 1, "squat" to listOf(set(1f, 1, 0)))), "bench"))
        assertNull(LastPerformance.lastSets(emptyList(), "bench"))
    }

    @Test
    fun `prefill repeats the final set and truncates`() {
        val last = listOf(set(80f, 8, 0), set(85f, 6, 1))
        assertEquals(listOf(80f, 85f, 85f), LastPerformance.prefill(last, 3).map { it?.weightKg })
        assertEquals(listOf(80f), LastPerformance.prefill(last, 1).map { it?.weightKg })
        assertEquals(listOf(null, null), LastPerformance.prefill(null, 2))
        assertEquals(emptyList<WorkoutSet?>(), LastPerformance.prefill(last, 0))
    }
}
