package com.rukavina.gymbuddy.domain.progress

import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.WorkoutSet
import com.rukavina.gymbuddy.testutil.ProgressFixtures.day
import com.rukavina.gymbuddy.testutil.ProgressFixtures.lift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Session detail rules (Plan 7 step 09a, R14-1/6). */
class SessionDetailTest {

    private fun run(vararg sets: Pair<Float, Int?>) = lift("run", name = "Running", type = ExerciseTrackingType.DISTANCE_DURATION)
        .copy(sets = sets.mapIndexed { i, (m, s) -> WorkoutSet(id = "r$i", distanceMeters = m, durationSeconds = s, isCompleted = true, orderIndex = i) })

    @Test
    fun weightsShowTimeSetsAndLifted() {
        val s = day("a", 0, lift("bench", 80f to 8, 82.5f to 8), lift("plank", null to null, type = ExerciseTrackingType.DURATION, order = 1), minutes = 52)
        assertEquals(SessionTiles.Lifted(52 * 60, 3, 80f * 8 + 82.5f * 8), SessionDetail.tiles(s))
    }

    @Test
    fun aRunShowsDistanceAndPace() {
        val s = day("r", 0, run(5200f to 1694), minutes = 31)
        val tiles = SessionDetail.tiles(s) as SessionTiles.Distance
        assertEquals(5200f, tiles.meters)
        assertEquals(1694 / 5.2f, tiles.secondsPerKm!!, 0.01f) // 5:26 per km
    }

    @Test
    fun paceUsesOnlyTimedDistanceAndIsNullWithoutTime() {
        val mixed = SessionDetail.tiles(day("r", 0, run(1000f to 300, 1000f to null))) as SessionTiles.Distance
        assertEquals(2000f, mixed.meters)
        assertEquals(300f, mixed.secondsPerKm!!, 0.01f)
        val untimed = SessionDetail.tiles(day("r", 0, run(1000f to null))) as SessionTiles.Distance
        assertNull(untimed.secondsPerKm)
    }

    @Test
    fun neitherWeightNorDistanceShowsTimeAndSets() {
        val s = day("p", 0, lift("pushup", null to 20, null to 15, type = ExerciseTrackingType.REPS_ONLY))
        assertEquals(SessionTiles.Plain(3600, 2), SessionDetail.tiles(s))
    }

    @Test
    fun cardsInOrderWithTopSetAndPerLiftPr() {
        val first = day("a", 0, lift("bench", 80f to 8), lift("squat", 100f to 5, order = 1))
        val second = day("b", 7, lift("squat", 100f to 5, order = 1), lift("bench", 80f to 8, 82.5f to 8, 82.5f to 6))
        val cards = SessionDetail.exercises(second, listOf(first, second))
        assertEquals(listOf("bench", "squat"), cards.map { it.performed.exerciseId })
        val bench = cards[0]
        assertEquals(82.5f, bench.top?.weightKg)
        assertEquals(8, bench.top?.reps)
        assertTrue(bench.isPr)
        assertFalse(cards[1].isPr) // same weight is no PR
        assertEquals(setOf("bench"), ProgressStats.prLifts(listOf(first, second))["b"])
        assertEquals(setOf("b"), ProgressStats.prSessions(listOf(first, second)))
    }

    @Test
    fun bodyweightTopIsMostReps() {
        val s = day("p", 0, lift("pushup", null to 12, null to 20, null to 15, type = ExerciseTrackingType.REPS_ONLY))
        assertEquals(20, SessionDetail.exercises(s, listOf(s)).single().top?.reps)
    }
}
