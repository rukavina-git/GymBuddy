package com.rukavina.gymbuddy.auth

import com.rukavina.gymbuddy.api.ErrorResponseWriter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Runs after FirebaseAuthenticationFilter in the chain, so the uid (if
 * any) is already in the SecurityContext. Rate limiting is per-uid, so
 * an unauthenticated request (no uid) is passed straight through - it's
 * about to be rejected with 401 anyway, and there is nothing to key a
 * bucket on.
 */
@Component
class RateLimitFilter(
    private val rateLimiter: TokenBucketRateLimiter,
    private val errorResponseWriter: ErrorResponseWriter,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(RateLimitFilter::class.java)

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val uid = SecurityContextHolder.getContext().authentication?.principal as? String

        if (uid == null) {
            filterChain.doFilter(request, response)
            return
        }

        val decision = rateLimiter.tryConsume(uid)
        if (!decision.allowed) {
            val traceId = errorResponseWriter.write(
                response = response,
                status = 429,
                error = "RATE_LIMITED",
                message = "Too many requests. Retry after ${decision.retryAfterSeconds}s.",
                retryAfterSeconds = decision.retryAfterSeconds,
            )
            log.warn("429 RATE_LIMITED [traceId={}] uid={} {} {}", traceId, uid, request.method, request.requestURI)
            return
        }

        filterChain.doFilter(request, response)
    }
}
