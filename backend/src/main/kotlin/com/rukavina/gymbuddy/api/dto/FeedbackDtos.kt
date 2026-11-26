package com.rukavina.gymbuddy.api.dto

/**
 * Mirrors FeedbackRequest in api/openapi.yaml. Every field is nullable
 * so a missing one reaches FeedbackService's checks and gets a clear
 * 400 message, instead of a generic "Malformed request body".
 */
data class FeedbackRequestDto(
    val id: String? = null,
    val message: String? = null,
    val appVersion: String? = null,
    val osVersion: String? = null,
    val deviceModel: String? = null,
    val createdAt: Long? = null,
)
