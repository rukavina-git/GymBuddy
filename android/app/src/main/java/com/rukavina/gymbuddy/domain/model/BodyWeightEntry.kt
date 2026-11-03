package com.rukavina.gymbuddy.domain.model

/**
 * One body weight measurement (Plan 6 step 04). Metric only: kg here,
 * imperial only at display. Several entries per day are allowed.
 */
data class BodyWeightEntry(
    val id: String,
    val measuredAt: Long,
    val weightKg: Float
)
