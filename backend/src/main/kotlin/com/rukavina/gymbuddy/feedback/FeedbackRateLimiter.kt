package com.rukavina.gymbuddy.feedback

import com.rukavina.gymbuddy.auth.RateLimitDecision
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

/**
 * At most [maxPerHour] feedback messages per key in any rolling hour.
 * Keys are "uid:<uid>" or "ip:<client ip>" for guests. In memory, like
 * TokenBucketRateLimiter (one instance). Separate from it because the
 * general limit (60/min) is far too loose for a text box, and it skips
 * guests. nginx adds a coarser per-IP limit in front.
 */
@Component
class FeedbackRateLimiter(
    @Value("\${gymbuddy.feedback.max-per-hour:5}") private val maxPerHour: Int,
) {
    /** Swapped by tests to move time; not a constructor arg so Spring needs no Clock bean. */
    internal var clock: Clock = Clock.systemUTC()

    private val windows = ConcurrentHashMap<String, ArrayDeque<Long>>()

    fun tryConsume(key: String): RateLimitDecision {
        val now = clock.millis()
        if (windows.size > MAX_KEYS) prune(now)
        val window = windows.computeIfAbsent(key) { ArrayDeque() }
        synchronized(window) {
            while (window.isNotEmpty() && now - window.peekFirst() >= HOUR_MS) window.pollFirst()
            if (window.size >= maxPerHour) {
                val retryAfterMs = HOUR_MS - (now - window.peekFirst())
                return RateLimitDecision(allowed = false, retryAfterSeconds = (retryAfterMs / 1000).coerceAtLeast(1))
            }
            window.addLast(now)
            return RateLimitDecision(allowed = true, retryAfterSeconds = 0)
        }
    }

    /** Drops keys with nothing in the last hour, so rotating IPs can't grow the map forever. */
    private fun prune(now: Long) {
        windows.entries.removeIf { (_, window) ->
            synchronized(window) { window.isEmpty() || now - window.peekLast() >= HOUR_MS }
        }
    }

    private companion object {
        const val HOUR_MS = 3_600_000L
        const val MAX_KEYS = 10_000
    }
}
