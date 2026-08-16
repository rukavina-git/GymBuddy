package com.rukavina.gymbuddy.auth

import com.rukavina.gymbuddy.api.ErrorResponseWriter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

/**
 * Spring Security's default is an empty 403 for an unauthenticated
 * request unless an entry point is configured - this replaces that with
 * a 401 in the api/openapi.yaml Error shape, for every rejection reason
 * (missing, malformed, expired, invalid token) since FirebaseAuthenticationFilter
 * folds them all into "no authentication in the context".
 */
@Component
class RestAuthenticationEntryPoint(private val errorResponseWriter: ErrorResponseWriter) : AuthenticationEntryPoint {

    private val logger = LoggerFactory.getLogger(RestAuthenticationEntryPoint::class.java)

    override fun commence(request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException) {
        val traceId = errorResponseWriter.write(
            response = response,
            status = HttpServletResponse.SC_UNAUTHORIZED,
            error = "UNAUTHENTICATED",
            message = "Missing, expired, or invalid authentication token.",
        )
        logger.info("401 UNAUTHENTICATED [traceId={}] {} {}", traceId, request.method, request.requestURI)
    }
}
