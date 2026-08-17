package com.rukavina.gymbuddy.sync

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

/**
 * Pure unit test, no Spring context - TombstoneRetentionScheduler is a
 * plain class over a constructor-injected TombstoneRetentionService, so
 * a bare Mockito mock is enough. Excluded under the "test" profile in
 * production wiring (see its class doc), which is exactly why nothing
 * else exercises it - this is its only coverage.
 */
class TombstoneRetentionSchedulerTest {

    private val service = mock(TombstoneRetentionService::class.java)
    private val scheduler = TombstoneRetentionScheduler(service)

    @Test
    fun `run delegates to the retention service`() {
        given(service.pruneExpiredTombstones()).willReturn(RetentionSummary(0, 0, 0))

        scheduler.run()

        verify(service).pruneExpiredTombstones()
    }

    @Test
    fun `run swallows an exception from the retention service rather than propagating it`() {
        given(service.pruneExpiredTombstones()).willThrow(RuntimeException("simulated failure"))

        // A failed scheduled run must not crash Spring's scheduling
        // thread - that would silently end every future run too, not
        // just this one. See TombstoneRetentionScheduler.run()'s catch.
        assertDoesNotThrow { scheduler.run() }
    }
}
