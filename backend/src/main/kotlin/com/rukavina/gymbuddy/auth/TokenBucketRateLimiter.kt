package com.rukavina.gymbuddy.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.min

/** Outcome of a single consumption attempt. */
data class RateLimitDecision(val allowed: Boolean, val retryAfterSeconds: Long)

/**
 * Per-uid token bucket, in-memory - fine for a single instance, per the
 * task. One bucket per uid, created lazily and kept for the process
 * lifetime; with a bounded user base this is an acceptable trade-off,
 * and there's no multi-instance deployment yet to require a shared
 * store (Redis, etc.).
 */
@Component
class TokenBucketRateLimiter(
    @Value("\${gymbuddy.rate-limit.capacity:60}") private val capacity: Long,
    @Value("\${gymbuddy.rate-limit.refill-per-minute:60}") private val refillPerMinute: Long,
) {
    private val buckets = ConcurrentHashMap<String, Bucket>()

    fun tryConsume(uid: String): RateLimitDecision {
        val bucket = buckets.computeIfAbsent(uid) { Bucket(capacity.toDouble(), refillPerMinute / 60.0) }
        return bucket.tryConsume()
    }

    private class Bucket(private val capacity: Double, private val refillPerSecond: Double) {
        private var tokens = capacity
        private var lastRefillNanos = System.nanoTime()

        @Synchronized
        fun tryConsume(): RateLimitDecision {
            refill()
            return if (tokens >= 1.0) {
                tokens -= 1.0
                RateLimitDecision(allowed = true, retryAfterSeconds = 0)
            } else {
                val secondsToNextToken = ceil((1.0 - tokens) / refillPerSecond).toLong().coerceAtLeast(1)
                RateLimitDecision(allowed = false, retryAfterSeconds = secondsToNextToken)
            }
        }

        private fun refill() {
            val now = System.nanoTime()
            val elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0
            tokens = min(capacity, tokens + elapsedSeconds * refillPerSecond)
            lastRefillNanos = now
        }
    }
}
