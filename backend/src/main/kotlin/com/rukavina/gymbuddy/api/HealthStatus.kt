package com.rukavina.gymbuddy.api

/** Mirrors the HealthStatus schema in api/openapi.yaml. */
data class HealthStatus(val status: String = "UP")
