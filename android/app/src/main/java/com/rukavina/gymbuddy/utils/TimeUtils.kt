package com.rukavina.gymbuddy.utils

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

fun getTimeBasedGreeting(birthDateMillis: Long? = null, clock: Clock = Clock.systemDefaultZone()): String {
    return try {
        // Check if today is the user's birthday
        if (birthDateMillis != null) {
            val birthDate = Instant.ofEpochMilli(birthDateMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val today = LocalDate.now(clock)
            if (birthDate.month == today.month && birthDate.dayOfMonth == today.dayOfMonth) {
                return "Happy Birthday"
            }
        }

        // Regular time-based greeting
        val currentHour = LocalTime.now(clock).hour
        when (currentHour) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..23 -> "Good evening"
            else -> "Hello" // Fallback
        }
    } catch (e: Exception) {
        "Hello" // Backup if something goes wrong
    }
}
