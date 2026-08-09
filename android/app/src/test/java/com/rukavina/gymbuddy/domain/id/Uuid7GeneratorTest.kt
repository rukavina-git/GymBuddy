package com.rukavina.gymbuddy.domain.id

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.random.Random

class Uuid7GeneratorTest {

    private fun clockAt(epochMs: Long): Clock =
        Clock.fixed(Instant.ofEpochMilli(epochMs), ZoneOffset.UTC)

    @Test
    fun `version nibble is 7`() {
        val uuid = Uuid7Generator(clockAt(1_754_745_600_000)).generate()
        assertEquals(7, uuid.version())
    }

    @Test
    fun `variant is RFC 4122`() {
        val uuid = Uuid7Generator(clockAt(1_754_745_600_000)).generate()
        assertEquals(2, uuid.variant())
    }

    @Test
    fun `embedded timestamp matches the clock`() {
        val epochMs = 1_754_745_600_000L
        val uuid = Uuid7Generator(clockAt(epochMs)).generate()
        val embedded = uuid.mostSignificantBits ushr 16
        assertEquals(epochMs, embedded)
    }

    @Test
    fun `ids sort lexicographically by creation time`() {
        val random = Random(42)
        val early = Uuid7Generator(clockAt(1_000_000_000_000), random).newId()
        val mid = Uuid7Generator(clockAt(1_500_000_000_000), random).newId()
        val late = Uuid7Generator(clockAt(2_000_000_000_000), random).newId()

        assertEquals(listOf(early, mid, late), listOf(late, early, mid).sorted())
    }

    @Test
    fun `ids are unique across many generations`() {
        val generator = Uuid7Generator(clockAt(1_754_745_600_000))
        val ids = (1..10_000).map { generator.newId() }
        assertEquals(10_000, ids.toSet().size)
    }

    @Test
    fun `string form is canonical`() {
        val id = Uuid7Generator(clockAt(1_754_745_600_000)).newId()
        assertEquals(36, id.length)
        assertTrue(id.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")))
    }
}