package com.rukavina.gymbuddy.ui.you.body

import com.rukavina.gymbuddy.domain.model.BodyWeightEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BodyWeightStatsTest {
    private val day = 86_400_000L
    private val now = 1_000 * day
    private fun e(daysAgo: Long, kg: Float) = BodyWeightEntry("id$daysAgo", now - daysAgo * day, kg)

    @Test
    fun `no change without an entry 30 days older than the latest`() {
        assertNull(BodyWeightStats.change(emptyList()))
        assertNull(BodyWeightStats.change(listOf(e(0, 80f), e(29, 82f))))
    }

    @Test
    fun `less and more compare with the newest entry at least 30 days older`() {
        assertEquals(-1.6f, BodyWeightStats.change(listOf(e(0, 82.4f), e(20, 83f), e(31, 84f), e(60, 90f)))!!, 0.001f)
        assertEquals(2f, BodyWeightStats.change(listOf(e(1, 82f), e(31, 80f)))!!, 0.001f)
        assertEquals(0f, BodyWeightStats.change(listOf(e(0, 80f), e(30, 80f)))!!, 0.001f)
    }

    @Test
    fun `range keeps entries inside the window, All keeps every one`() {
        val entries = listOf(e(0, 1f), e(30, 2f), e(31, 3f), e(100, 4f), e(400, 5f))
        assertEquals(listOf(1f, 2f), BodyWeightStats.inRange(entries, WeightRange.Days30, now).map { it.weightKg })
        assertEquals(listOf(1f, 2f, 3f), BodyWeightStats.inRange(entries, WeightRange.Days90, now).map { it.weightKg })
        assertEquals(4, BodyWeightStats.inRange(entries, WeightRange.Year, now).size)
        assertEquals(5, BodyWeightStats.inRange(entries, WeightRange.All, now).size)
    }
}
