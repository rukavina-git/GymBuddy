package com.rukavina.gymbuddy.feedback

import com.rukavina.gymbuddy.api.dto.FeedbackRequestDto
import com.rukavina.gymbuddy.persistence.FeedbackRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/** A FeedbackRequest that breaks the spec's rules. Mapped to 400 VALIDATION_FAILED. */
class FeedbackValidationException(message: String) : RuntimeException(message)

const val FEEDBACK_MAX_CHARS = 2000

/**
 * Checks and stores one feedback message (POST /v1/feedback). The
 * message is stored trimmed. A known id stores nothing and pings
 * nobody, so client retries are harmless.
 */
@Service
class FeedbackService(
    private val repository: FeedbackRepository,
    private val notifier: FeedbackNotifier,
) {

    fun submit(uid: String?, request: FeedbackRequestDto) {
        val id = request.id?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: throw FeedbackValidationException("id must be a UUID.")
        val message = request.message?.trim()
        if (message.isNullOrEmpty()) throw FeedbackValidationException("message must not be blank.")
        if (message.length > FEEDBACK_MAX_CHARS) {
            throw FeedbackValidationException("message must be at most $FEEDBACK_MAX_CHARS characters.")
        }
        val createdAt = request.createdAt ?: throw FeedbackValidationException("createdAt is required.")
        val appVersion = optional(request.appVersion, "appVersion", 32)
        val osVersion = optional(request.osVersion, "osVersion", 32)
        val deviceModel = optional(request.deviceModel, "deviceModel", 64)

        val inserted = repository.insertIfAbsent(
            id = id,
            userId = uid,
            message = message,
            appVersion = appVersion,
            osVersion = osVersion,
            deviceModel = deviceModel,
            createdAt = Instant.ofEpochMilli(createdAt),
        )
        if (inserted) notifier.newFeedback()
    }

    private fun optional(value: String?, name: String, max: Int): String? {
        val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (trimmed.length > max) throw FeedbackValidationException("$name must be at most $max characters.")
        return trimmed
    }
}
