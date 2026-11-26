package com.rukavina.gymbuddy.feedback

import com.rukavina.gymbuddy.api.dto.AdminFeedbackCountsDto
import com.rukavina.gymbuddy.api.dto.AdminFeedbackItemDto
import com.rukavina.gymbuddy.api.dto.AdminFeedbackPageDto
import com.rukavina.gymbuddy.api.dto.AdminFeedbackUpdateDto
import com.rukavina.gymbuddy.persistence.FeedbackPosition
import com.rukavina.gymbuddy.persistence.FeedbackRepository
import com.rukavina.gymbuddy.persistence.FeedbackRow
import com.rukavina.gymbuddy.sync.CursorDecodeException
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Base64
import java.util.UUID

/** PATCH on an id that isn't stored. Mapped to 404 NOT_FOUND. */
class FeedbackNotFoundException(id: String) : RuntimeException("No feedback with id $id.")

val FEEDBACK_STATUSES = listOf("open", "solved", "wont_fix", "spam")
const val FEEDBACK_NOTE_MAX_CHARS = 500
const val ADMIN_FEEDBACK_LIMIT_DEFAULT = 20
const val ADMIN_FEEDBACK_LIMIT_MAX = 100

/**
 * The admin page's view of feedback (GET/PATCH /v1/admin/feedback).
 * Who may call it is decided once, in AdminAccess (SecurityConfig),
 * not here. The cursor is opaque to clients: base64url of
 * "<receivedAt ISO instant>|<id>", the keyset of the last row.
 */
@Service
class FeedbackAdminService(private val repository: FeedbackRepository) {

    fun list(status: String?, cursor: String?, limit: Int?): AdminFeedbackPageDto {
        val statusFilter = status?.takeIf { it.isNotBlank() }?.also { checkStatus(it) }
        // Clamped, not rejected, like /v1/sync/pull's limit.
        val pageSize = (limit ?: ADMIN_FEEDBACK_LIMIT_DEFAULT).coerceIn(1, ADMIN_FEEDBACK_LIMIT_MAX)
        val after = cursor?.takeIf { it.isNotBlank() }?.let(::decodeCursor)

        // One extra row tells whether another page exists.
        val rows = repository.list(statusFilter, after, pageSize + 1)
        val page = rows.take(pageSize)
        val next = if (rows.size > pageSize) encodeCursor(page.last()) else null

        val counts = repository.countByStatus()
        return AdminFeedbackPageDto(
            items = page.map { it.toDto() },
            nextCursor = next,
            counts = AdminFeedbackCountsDto(
                open = counts["open"] ?: 0,
                solved = counts["solved"] ?: 0,
                wontFix = counts["wont_fix"] ?: 0,
                spam = counts["spam"] ?: 0,
            ),
        )
    }

    fun update(rawId: String, request: AdminFeedbackUpdateDto): AdminFeedbackItemDto {
        val id = runCatching { UUID.fromString(rawId) }.getOrNull()
            ?: throw FeedbackValidationException("id must be a UUID.")
        if (request.status == null && request.note == null) {
            throw FeedbackValidationException("Send status, note or both.")
        }
        request.status?.let(::checkStatus)
        val note = request.note?.trim()
        if (note != null && note.length > FEEDBACK_NOTE_MAX_CHARS) {
            throw FeedbackValidationException("note must be at most $FEEDBACK_NOTE_MAX_CHARS characters.")
        }
        val row = repository.update(
            id = id,
            status = request.status,
            note = note?.takeIf { it.isNotEmpty() },
            clearNote = note != null && note.isEmpty(),
        ) ?: throw FeedbackNotFoundException(rawId)
        return row.toDto()
    }

    private fun checkStatus(status: String) {
        if (status !in FEEDBACK_STATUSES) {
            throw FeedbackValidationException("status must be one of ${FEEDBACK_STATUSES.joinToString()}.")
        }
    }

    private fun encodeCursor(row: FeedbackRow): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString("${row.receivedAt}|${row.id}".toByteArray())

    private fun decodeCursor(raw: String): FeedbackPosition = try {
        val (instant, id) = String(Base64.getUrlDecoder().decode(raw)).split('|', limit = 2)
        FeedbackPosition(Instant.parse(instant), UUID.fromString(id))
    } catch (e: Exception) {
        throw CursorDecodeException("Malformed feedback cursor.", e)
    }

    private fun FeedbackRow.toDto() = AdminFeedbackItemDto(
        id = id.toString(),
        message = message,
        status = status,
        note = note,
        account = userId ?: "guest",
        appVersion = appVersion,
        osVersion = osVersion,
        deviceModel = deviceModel,
        createdAt = createdAt.toEpochMilli(),
        receivedAt = receivedAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
    )
}
