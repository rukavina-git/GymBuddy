package com.rukavina.gymbuddy.api.dto

import com.fasterxml.jackson.annotation.JsonProperty

/** Mirrors AdminFeedbackItem in api/openapi.yaml. account is "guest" or the Firebase uid. */
data class AdminFeedbackItemDto(
    val id: String,
    val message: String,
    val status: String,
    val note: String?,
    val account: String,
    val appVersion: String?,
    val osVersion: String?,
    val deviceModel: String?,
    val createdAt: Long,
    val receivedAt: Long,
    val updatedAt: Long,
)

/** Mirrors AdminFeedbackCounts: rows per status, all statuses present (0 when none). */
data class AdminFeedbackCountsDto(
    val open: Long,
    val solved: Long,
    @get:JsonProperty("wont_fix")
    val wontFix: Long,
    val spam: Long,
)

/** Mirrors AdminFeedbackPage. nextCursor null = no more rows. */
data class AdminFeedbackPageDto(
    val items: List<AdminFeedbackItemDto>,
    val nextCursor: String?,
    val counts: AdminFeedbackCountsDto,
)

/**
 * Mirrors AdminFeedbackUpdate. Absent or null = unchanged; an empty
 * (or blank) note clears it. Nullable so a bad body gets a clear 400.
 */
data class AdminFeedbackUpdateDto(
    val status: String? = null,
    val note: String? = null,
)
