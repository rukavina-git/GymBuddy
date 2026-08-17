package com.rukavina.gymbuddy.sync

/**
 * Thrown when a pull cursor predates the oldest change_log row this
 * user currently has (point 5) - the client has missed changes that no
 * longer exist to be replayed. Maps to 410 CURSOR_EXPIRED - see
 * GlobalExceptionHandler. The client's only correct response is to
 * discard all local data for this user and start over with a full sync
 * (pull again with no cursor).
 */
class CursorExpiredException(message: String) : RuntimeException(message)
