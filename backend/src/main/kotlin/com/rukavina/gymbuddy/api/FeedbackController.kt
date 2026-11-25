package com.rukavina.gymbuddy.api

import com.rukavina.gymbuddy.api.dto.FeedbackRequestDto
import com.rukavina.gymbuddy.feedback.FeedbackRateLimiter
import com.rukavina.gymbuddy.feedback.FeedbackService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * POST /v1/feedback per api/openapi.yaml. The one write endpoint open
 * to guests (SecurityConfig permits it): the uid is used when a valid
 * token came with the request, otherwise the message has no account.
 * Rate limited per uid, or per client IP for guests. X-Real-IP is set
 * by nginx (overwriting any client value); the backend has no public
 * port, so it can't be spoofed from outside.
 */
@RestController
class FeedbackController(
    private val feedbackService: FeedbackService,
    private val rateLimiter: FeedbackRateLimiter,
    private val errorResponseWriter: ErrorResponseWriter,
) {
    private val log = LoggerFactory.getLogger(FeedbackController::class.java)

    @PostMapping("/v1/feedback")
    fun sendFeedback(
        @RequestBody body: FeedbackRequestDto,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Unit>? {
        // Guests reach here with Spring's anonymous token, whose principal
        // is the string "anonymousUser": never treat that as a uid.
        val uid = SecurityContextHolder.getContext().authentication
            ?.takeUnless { it is AnonymousAuthenticationToken }
            ?.principal as? String
        val key = if (uid != null) "uid:$uid" else "ip:${clientIp(request)}"
        val decision = rateLimiter.tryConsume(key)
        if (!decision.allowed) {
            val traceId = errorResponseWriter.write(
                response = response,
                status = 429,
                error = "RATE_LIMITED",
                message = "Too much feedback. Retry after ${decision.retryAfterSeconds}s.",
                retryAfterSeconds = decision.retryAfterSeconds,
            )
            log.warn("429 RATE_LIMITED [traceId={}] feedback guest={}", traceId, uid == null)
            return null
        }
        feedbackService.submit(uid, body)
        return ResponseEntity.noContent().build()
    }

    private fun clientIp(request: HttpServletRequest): String =
        request.getHeader("X-Real-IP")?.trim()?.takeIf { it.isNotEmpty() } ?: request.remoteAddr
}
