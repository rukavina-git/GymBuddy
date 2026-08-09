package com.rukavina.gymbuddy.domain.id

import java.time.Clock
import java.util.UUID
import kotlin.random.Random

/**
 * UUID version 7 generator (RFC 9562).
 *
 * Layout:
 *   bits 127..80  unix_ts_ms  (48 bits, big-endian)
 *   bits  79..76  version     (0111)
 *   bits  75..64  rand_a      (12 bits)
 *   bits  63..62  variant     (10)
 *   bits  61..0   rand_b      (62 bits)
 *
 * The embedded timestamp makes identifiers sort by creation time,
 * which keeps B-tree inserts local in Postgres and makes ordering
 * readable off the ID itself.
 *
 * Deliberate simplification: rand_a is random rather than a
 * monotonic counter (RFC 9562 §6.2 Method 3). Ordering within a
 * single millisecond is not guaranteed. For this application's
 * write rate — a handful of entities per user action — that is
 * an acceptable trade for a simpler implementation.
 */
class Uuid7Generator(
    private val clock: Clock,
    private val random: Random = Random.Default
) : IdGenerator {

    override fun newId(): String = generate().toString()

    fun generate(): UUID {
        val timestampMs = clock.millis() and TIMESTAMP_MASK
        val randA = random.nextInt(1 shl 12).toLong()

        val mostSignificantBits =
            (timestampMs shl 16) or (VERSION_7 shl 12) or randA

        val leastSignificantBits =
            (random.nextLong() and RAND_B_MASK) or VARIANT_RFC4122

        return UUID(mostSignificantBits, leastSignificantBits)
    }

    private companion object {
        const val TIMESTAMP_MASK = 0x0000_FFFF_FFFF_FFFFL
        const val VERSION_7 = 0x7L
        const val RAND_B_MASK = 0x3FFF_FFFF_FFFF_FFFFL
        const val VARIANT_RFC4122 = 1L shl 63
    }
}