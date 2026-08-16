package com.rukavina.gymbuddy.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.rukavina.gymbuddy.api.dto.ErrorDto
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Shared by every component that has to write an Error-shaped body
 * directly to a servlet response instead of returning through a
 * @RestController (the Spring Security entry point/access-denied
 * handler, and the rate limit filter) - all three sit outside the
 * @ControllerAdvice's reach since it only intercepts exceptions thrown
 * from controller methods. Keeps the JSON shape, UTF-8 charset, and
 * traceId generation identical across all of them.
 */
@Component
class ErrorResponseWriter(private val objectMapper: ObjectMapper) {

    fun write(response: HttpServletResponse, status: Int, error: String, message: String, retryAfterSeconds: Long? = null): String {
        val traceId = UUID.randomUUID().toString()
        response.status = status
        response.characterEncoding = StandardCharsets.UTF_8.name()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        retryAfterSeconds?.let { response.setHeader("Retry-After", it.toString()) }
        objectMapper.writeValue(response.writer, ErrorDto(error = error, message = message, traceId = traceId))
        return traceId
    }
}
