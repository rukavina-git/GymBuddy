package com.rukavina.gymbuddy.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class TimeUtilsTest {

    private val zone = ZoneId.systemDefault()

    private fun clockAt(date: LocalDate, hour: Int): Clock =
        Clock.fixed(date.atTime(hour, 0).atZone(zone).toInstant(), zone)

    private fun millisFor(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun `morning hours greet Good morning`() {
        val clock = clockAt(LocalDate.of(2026, 3, 10), hour = 8)

        assertEquals("Good morning", getTimeBasedGreeting(clock = clock))
    }

    @Test
    fun `midnight is still Good morning - lower bound of the range`() {
        val clock = clockAt(LocalDate.of(2026, 3, 10), hour = 0)

        assertEquals("Good morning", getTimeBasedGreeting(clock = clock))
    }

    @Test
    fun `11am is still Good morning - upper bound of the range`() {
        val clock = clockAt(LocalDate.of(2026, 3, 10), hour = 11)

        assertEquals("Good morning", getTimeBasedGreeting(clock = clock))
    }

    @Test
    fun `afternoon hours greet Good afternoon`() {
        val clock = clockAt(LocalDate.of(2026, 3, 10), hour = 14)

        assertEquals("Good afternoon", getTimeBasedGreeting(clock = clock))
    }

    @Test
    fun `evening hours greet Good evening`() {
        val clock = clockAt(LocalDate.of(2026, 3, 10), hour = 20)

        assertEquals("Good evening", getTimeBasedGreeting(clock = clock))
    }

    @Test
    fun `11pm is still Good evening - upper bound of the range`() {
        val clock = clockAt(LocalDate.of(2026, 3, 10), hour = 23)

        assertEquals("Good evening", getTimeBasedGreeting(clock = clock))
    }

    @Test
    fun `birthday overrides the time-based greeting`() {
        val today = LocalDate.of(2026, 3, 10)
        val clock = clockAt(today, hour = 14) // would otherwise be "Good afternoon"
        val birthDateMillis = millisFor(today.minusYears(30))

        assertEquals("Happy Birthday", getTimeBasedGreeting(birthDateMillis, clock))
    }

    @Test
    fun `birthday matches on month and day only, ignoring year`() {
        val today = LocalDate.of(2026, 3, 10)
        val clock = clockAt(today, hour = 8)
        val birthDateMillis = millisFor(LocalDate.of(1975, 3, 10))

        assertEquals("Happy Birthday", getTimeBasedGreeting(birthDateMillis, clock))
    }

    @Test
    fun `a birth date on a different day does not trigger the birthday greeting`() {
        val today = LocalDate.of(2026, 3, 10)
        val clock = clockAt(today, hour = 14)
        val birthDateMillis = millisFor(LocalDate.of(1996, 3, 11))

        assertEquals("Good afternoon", getTimeBasedGreeting(birthDateMillis, clock))
    }

    @Test
    fun `a null birth date is treated as no birthday`() {
        val clock = clockAt(LocalDate.of(2026, 3, 10), hour = 8)

        assertEquals("Good morning", getTimeBasedGreeting(birthDateMillis = null, clock = clock))
    }
}
