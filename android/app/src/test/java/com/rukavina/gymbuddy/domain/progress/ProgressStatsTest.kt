package com.rukavina.gymbuddy.domain.progress

import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.testutil.ProgressFixtures.day
import com.rukavina.gymbuddy.testutil.ProgressFixtures.lift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class ProgressStatsTest {

    private val utc = ZoneOffset.UTC

    @Test
    fun eligibleNeedsThreeWeightedSessionsOverTwoWeeks() {
        val sessions = listOf(
            day("a", 0, lift("bench", 80f to 5), lift("plank", null to null, type = ExerciseTrackingType.DURATION)),
            day("b", 7, lift("bench", 82.5f to 5), lift("squat", 100f to 5)),
            day("c", 14, lift("bench", 85f to 5), lift("squat", 105f to 5), lift("pullup", null to 8, type = ExerciseTrackingType.REPS_ONLY)),
            day("d", 15, lift("squat", 110f to 5)),
            day("e", 16, lift("pullup", null to 8, type = ExerciseTrackingType.REPS_ONLY)),
        )
        // squat: 3 sessions but only 8 days apart; pullup and plank: no weight type.
        assertEquals(listOf("bench"), ProgressStats.eligibleLifts(sessions).map { it.exerciseId })
    }

    @Test
    fun weightedTypeWithoutWeightDoesNotCount() {
        val sessions = (0..2).map { day("s$it", it * 7, lift("dip", null to 10)) }
        assertTrue(ProgressStats.eligibleLifts(sessions).isEmpty())
    }

    @Test
    fun deletedSessionsDoNotCount() {
        val sessions = listOf(
            day("a", 0, lift("bench", 80f to 5)),
            day("b", 7, lift("bench", 80f to 5), deletedAt = 1),
            day("c", 14, lift("bench", 80f to 5)),
        )
        assertTrue(ProgressStats.eligibleLifts(sessions).isEmpty())
    }

    @Test
    fun liftsSortByCountThenNameAndStoredOneWins() {
        val sessions = (0..3).map { i ->
            if (i < 3) day("s$i", i * 7, lift("row", 60f to 8, name = "Row"), lift("bench", 80f to 5, name = "Bench"), lift("dl", 140f to 3, name = "Deadlift"))
            else day("s$i", i * 7, lift("dl", 140f to 3, name = "Deadlift"))
        }
        val lifts = ProgressStats.eligibleLifts(sessions)
        assertEquals(listOf("dl", "bench", "row"), lifts.map { it.exerciseId })
        assertEquals(4, lifts.first().sessionCount)
        assertEquals("row", ProgressStats.chosenLift(lifts, "row")?.exerciseId)
        assertEquals("dl", ProgressStats.chosenLift(lifts, "gone")?.exerciseId) // fallback
        assertEquals("dl", ProgressStats.chosenLift(lifts, null)?.exerciseId)
        assertNull(ProgressStats.chosenLift(emptyList(), "row"))
    }

    @Test
    fun progressUsesHeaviestSetAndEightWeekWindow() {
        val sessions = listOf(
            day("old", 0, lift("bench", 60f to 5)),
            day("a", 70, lift("bench", 80f to 5, 82.5f to 3)),
            day("b", 84, lift("bench", 85f to 5)),
            day("c", 126, lift("bench", 87.5f to 5)),
        )
        val p = ProgressStats.progress(Lift("bench", "Bench", 4), sessions)!!
        assertEquals(listOf("a", "b", "c"), p.points.map { it.sessionId }) // "old" is outside 8 weeks
        assertEquals(82.5f, p.first)
        assertEquals(5f, p.change)
        assertEquals(8, p.weeks)
    }

    @Test
    fun thinWindowFillsInOlderSessions() {
        val sessions = listOf(
            day("a", 0, lift("bench", 70f to 5)),
            day("b", 14, lift("bench", 75f to 5)),
            day("c", 200, lift("bench", 80f to 5)),
        )
        val p = ProgressStats.progress(Lift("bench", "Bench", 3), sessions)!!
        assertEquals(3, p.points.size)
        assertEquals(10f, p.change)
    }

    @Test
    fun weekCountsMondayToSundayWithVolume() {
        val wednesday = LocalDate.of(1970, 1, 7) // day 6; Monday is day 4
        val sessions = listOf(
            day("sun", 3, lift("bench", 100f to 5)), // last week
            day("mon", 4, lift("bench", 80f to 5, 80f to 5), minutes = 30),
            day("wed", 6, lift("squat", 100f to 3), lift("plank", null to null, type = ExerciseTrackingType.DURATION), minutes = 45),
        )
        val w = ProgressStats.week(sessions, wednesday, utc)
        assertEquals(2, w.sessions)
        assertEquals(75 * 60L, w.seconds)
        assertEquals(1100f, w.volumeKg)
    }

    @Test
    fun prIsABetterHeaviestSetThanAnyEarlierSession() {
        val sessions = listOf(
            day("a", 0, lift("bench", 80f to 5)),
            day("b", 7, lift("bench", 80f to 5)), // same: no PR
            day("c", 14, lift("bench", 80f to 6)), // more reps at the top weight
            day("d", 21, lift("bench", 75f to 10), lift("squat", 100f to 5)), // first squat: no PR
        )
        assertEquals(setOf("c"), ProgressStats.prSessions(sessions))
    }

    @Test
    fun topSetIsHeaviestWeightedElseFirstExercise() {
        val s = day("a", 0, lift("curl", 20f to 10, order = 0), lift("squat", 110f to 5, order = 1))
        assertEquals("squat", ProgressStats.topSet(s)?.first?.exerciseId)
        val bodyweight = day("b", 0, lift("pullup", null to 8, type = ExerciseTrackingType.REPS_ONLY, order = 0))
        assertEquals(8, ProgressStats.topSet(bodyweight)?.second?.reps)
        assertNull(ProgressStats.topSet(day("c", 0)))
    }

    @Test
    fun monthsNewestFirstWithVolume() {
        val sessions = listOf(
            day("jan1", 0, lift("bench", 100f to 1)),
            day("jan2", 10, lift("bench", 100f to 2)),
            day("feb", 40, lift("bench", 50f to 2)),
        )
        val months = ProgressStats.byMonth(sessions, utc)
        assertEquals(listOf(YearMonth.of(1970, 2), YearMonth.of(1970, 1)), months.map { it.month })
        assertEquals(listOf("jan2", "jan1"), months[1].sessions.map { it.id })
        assertEquals(300f, months[1].volumeKg)
    }

    @Test
    fun loggedExercisesAndSetsOf() {
        val s = day("a", 0, lift("bench", 80f to 5, 85f to 3, name = "Bench"), lift("bench", 60f to 10, name = "Bench", order = 1))
        val logged = ProgressStats.loggedExercises(listOf(s, day("b", 1, lift("row", 50f to 8))))
        assertEquals(setOf("bench", "row"), logged.map { it.exerciseId }.toSet())
        assertEquals(1, logged.first { it.exerciseId == "bench" }.sessionCount)
        assertEquals(listOf(80f, 85f, 60f), ProgressStats.setsOf(s, "bench").map { it.weightKg })
    }

    @Test
    fun yTicksAreRoundAndCover() {
        assertEquals(listOf(80f, 85f, 90f), ProgressStats.yTicks(80f, 87.5f))
        assertEquals(listOf(100f, 110f, 120f), ProgressStats.yTicks(102f, 118f))
        val flat = ProgressStats.yTicks(80f, 80f)
        assertTrue(flat.first() <= 80f && flat.last() >= 80f)
        val wide = ProgressStats.yTicks(40f, 140f)
        assertTrue(wide.first() <= 40f && wide.last() >= 140f)
    }

    @Test
    fun dragSnapsToNearestPoint() {
        assertEquals(0, ProgressStats.nearestIndex(-5f, 300f, 4))
        assertEquals(1, ProgressStats.nearestIndex(120f, 300f, 4)) // step 100
        assertEquals(2, ProgressStats.nearestIndex(151f, 300f, 4))
        assertEquals(3, ProgressStats.nearestIndex(999f, 300f, 4))
        assertEquals(0, ProgressStats.nearestIndex(50f, 300f, 1))
    }
}
