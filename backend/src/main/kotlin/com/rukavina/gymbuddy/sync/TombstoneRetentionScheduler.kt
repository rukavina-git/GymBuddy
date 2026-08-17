package com.rukavina.gymbuddy.sync

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Daily trigger for TombstoneRetentionService - point 2's "scheduled
 * daily". Excluded under the "test" profile so Testcontainers tests
 * control pruning explicitly by calling the service directly, rather
 * than racing a real timer; @EnableScheduling (GymBuddyApplication) is
 * harmless to leave on globally since there's nothing else @Scheduled
 * for it to trigger there.
 */
@Component
@Profile("!test")
class TombstoneRetentionScheduler(private val tombstoneRetentionService: TombstoneRetentionService) {

    private val log = LoggerFactory.getLogger(TombstoneRetentionScheduler::class.java)

    @Scheduled(cron = "\${gymbuddy.sync.retention-cron:0 0 3 * * *}")
    fun run() {
        try {
            tombstoneRetentionService.pruneExpiredTombstones()
        } catch (e: Exception) {
            log.error("Scheduled tombstone retention pass failed - will retry on the next run.", e)
        }
    }
}
