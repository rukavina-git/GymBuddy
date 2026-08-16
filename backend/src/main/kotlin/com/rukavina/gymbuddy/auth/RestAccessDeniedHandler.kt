package com.rukavina.gymbuddy.auth

import com.rukavina.gymbuddy.api.ErrorResponseWriter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

/** Same rationale as RestAuthenticationEntryPoint, for the authenticated-but-forbidden case. */
@Component
class RestAccessDeniedHandler(private val errorResponseWriter: ErrorResponseWriter) : AccessDeniedHandler {

    private val logger = LoggerFactory.getLogger(RestAccessDeniedHandler::class.java)

    override fun handle(request: HttpServletRequest, response: HttpServletResponse, accessDeniedException: AccessDeniedException) {
        val traceId = errorResponseWriter.write(
            response = response,
            status = HttpServletResponse.SC_FORBIDDEN,
            error = "FORBIDDEN",
            message = "Not permitted to perform this action.",
        )
        logger.info("403 FORBIDDEN [traceId={}] {} {}", traceId, request.method, request.requestURI)
    }
}
