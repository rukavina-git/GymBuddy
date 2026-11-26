package com.rukavina.gymbuddy.feedback

import com.rukavina.gymbuddy.persistence.FeedbackRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/**
 * Deletes feedback received more than [retentionDays] ago, any status.
 * The privacy policy promises 2 years (Plan 10 step 06c).
 */
@Service
class FeedbackRetentionService(
    private val repository: FeedbackRepository,
    @Value("\${gymbuddy.feedback.retention-days:730}") private val retentionDays: Long,
) {
    private val log = LoggerFactory.getLogger(FeedbackRetentionService::class.java)

    fun pruneExpired(now: Instant = Instant.now()): Int {
        val deleted = repository.deleteReceivedBefore(now.minus(Duration.ofDays(retentionDays)))
        if (deleted > 0) log.info("Feedback retention deleted {} row(s) older than {} days", deleted, retentionDays)
        return deleted
    }
}

/** Daily trigger, off under "test" like TombstoneRetentionScheduler. */
@Component
@Profile("!test")
class FeedbackRetentionScheduler(private val service: FeedbackRetentionService) {

    private val log = LoggerFactory.getLogger(FeedbackRetentionScheduler::class.java)

    @Scheduled(cron = "\${gymbuddy.feedback.retention-cron:0 30 3 * * *}")
    fun run() {
        try {
            service.pruneExpired()
        } catch (e: Exception) {
            log.error("Feedback retention pass failed - will retry on the next run.", e)
        }
    }
}
