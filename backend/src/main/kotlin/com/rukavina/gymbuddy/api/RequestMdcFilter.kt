package com.rukavina.gymbuddy.api

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * MDC keys every log line of a request carries. JSON logs (ECS) write
 * them as top-level fields, so one traceId finds the whole request.
 * userId is the Firebase uid only, set by FirebaseAuthenticationFilter.
 */
object RequestContext {
    const val TRACE_ID = "traceId"
    const val USER_ID = "userId"
    const val CLIENT_VERSION = "clientVersion"
    const val CLIENT_PLATFORM = "clientPlatform"

    const val CLIENT_VERSION_HEADER = "X-Client-Version"
    const val CLIENT_PLATFORM_HEADER = "X-Client-Platform"

    /**
     * The current request's traceId, so an error body and its log line
     * match. Falls back to a fresh one outside a request.
     */
    fun traceId(): String = MDC.get(TRACE_ID) ?: UUID.randomUUID().toString()
}

private const val MAX_HEADER_LENGTH = 64

/**
 * First filter in the chain, ahead of Spring Security: assigns the
 * request's traceId and copies the client headers into MDC, then clears
 * everything (including userId) once the request is done, since servlet
 * threads are pooled.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestMdcFilter : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        MDC.put(RequestContext.TRACE_ID, UUID.randomUUID().toString())
        request.getHeader(RequestContext.CLIENT_VERSION_HEADER)?.let { MDC.put(RequestContext.CLIENT_VERSION, sanitize(it)) }
        request.getHeader(RequestContext.CLIENT_PLATFORM_HEADER)?.let { MDC.put(RequestContext.CLIENT_PLATFORM, sanitize(it)) }
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(RequestContext.TRACE_ID)
            MDC.remove(RequestContext.USER_ID)
            MDC.remove(RequestContext.CLIENT_VERSION)
            MDC.remove(RequestContext.CLIENT_PLATFORM)
        }
    }

    // Client-supplied, so cap the length and drop control characters
    // before it reaches a plain-text log line.
    private fun sanitize(value: String): String =
        value.filterNot { it.isISOControl() }.take(MAX_HEADER_LENGTH)
}
