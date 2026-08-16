package com.rukavina.gymbuddy.api.dto

/** Mirrors the Error schema in api/openapi.yaml - the body for every non-2xx response. */
data class ErrorDto(
    val error: String,
    val message: String,
    val details: Map<String, Any?>? = null,
    val traceId: String,
)
