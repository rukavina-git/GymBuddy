package com.rukavina.gymbuddy.testutil

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** A Clock a test can move forward. */
class MutableClock(var millis: Long = 1_767_225_600_000L) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = Instant.ofEpochMilli(millis)
    override fun millis(): Long = millis

    fun advanceSeconds(seconds: Long) {
        millis += seconds * 1000
    }
}
