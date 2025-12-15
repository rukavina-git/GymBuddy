package com.rukavina.gymbuddy.utils

fun getTimeBasedGreeting(): String {
    return try {
        val currentHour = java.time.LocalTime.now().hour
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
