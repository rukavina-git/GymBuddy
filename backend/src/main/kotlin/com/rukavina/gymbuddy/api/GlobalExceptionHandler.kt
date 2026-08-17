package com.rukavina.gymbuddy.api

import com.rukavina.gymbuddy.api.dto.ErrorDto
import com.rukavina.gymbuddy.sync.CursorDecodeException
import com.rukavina.gymbuddy.sync.CursorExpiredException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.UUID

/**
 * Maps uncaught controller exceptions to the Error shape in
 * api/openapi.yaml, with a fresh traceId logged alongside the
 * server-side entry so a report of "traceId X failed" is greppable.
 * This only ever sees exceptions thrown from controller method
 * execution - Spring Security's entry point/access-denied handler and
 * the rate limit filter run outside its reach and use
 * ErrorResponseWriter directly for the same shape.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(CursorDecodeException::class)
    fun handleCursorDecode(ex: CursorDecodeException, request: HttpServletRequest): ResponseEntity<ErrorDto> {
        val traceId = UUID.randomUUID().toString()
        logger.debug("400 VALIDATION_FAILED [traceId={}] {} {}: {}", traceId, request.method, request.requestURI, ex.message)
        val body = ErrorDto(error = "VALIDATION_FAILED", message = "Malformed cursor.", traceId = traceId)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
    }

    @ExceptionHandler(CursorExpiredException::class)
    fun handleCursorExpired(ex: CursorExpiredException, request: HttpServletRequest): ResponseEntity<ErrorDto> {
        val traceId = UUID.randomUUID().toString()
        logger.info("410 CURSOR_EXPIRED [traceId={}] {} {}: {}", traceId, request.method, request.requestURI, ex.message)
        val body = ErrorDto(
            error = "CURSOR_EXPIRED",
            message = "Cursor predates the retained change log. Discard local data and perform a full sync.",
            traceId = traceId,
        )
        return ResponseEntity.status(HttpStatus.GONE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception, request: HttpServletRequest): ResponseEntity<ErrorDto> {
        val traceId = UUID.randomUUID().toString()
        logger.error("500 INTERNAL [traceId={}] {} {}", traceId, request.method, request.requestURI, ex)
        val body = ErrorDto(error = "INTERNAL", message = "An unexpected error occurred.", traceId = traceId)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
    }
}
