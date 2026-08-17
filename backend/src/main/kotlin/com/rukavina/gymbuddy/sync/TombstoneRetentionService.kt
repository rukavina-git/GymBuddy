package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.domain.EntityType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.TimeUnit

/** Outcome of one retention pass, for logging/tests - not part of any API response. */
data class RetentionSummary(val usersProcessed: Int, val entitiesDeleted: Int, val changeLogRowsDeleted: Int)

private data class TombstonedTable(val entityType: EntityType, val tableName: String, val idColumn: String)

/**
 * Hard-deletes tombstoned entities past the retention window, and every
 * change_log row that references them - point 2's whole reason for
 * being paired with point 3: a tombstone whose change_log row survives
 * would be delivered to some future pull as "this entity changed" when
 * the entity no longer exists to be read back, and the log would never
 * shrink. Also advances the per-user retention watermark (point 3) that
 * closes Group G's cursor-expiry gap - see sync_retention_watermark's
 * migration comment and ChangeLogReader.isCursorExpired.
 *
 * One entity type at a time is a shared, single implementation rather
 * than three copies: TOMBSTONED_TABLES is the only place that knows
 * which tables carry a deletedAt (user_profile deliberately excluded -
 * account deletion hard-deletes it immediately, it never carries a
 * push-driven tombstone; user_exercise_state/user_template_state have
 * no deletedAt at all, upsert-only). Per-user transactions, each taking
 * the same advisory lock the push path uses, are what point 2 means by
 * "safe to run concurrently with sync" and "delete in batches" - a
 * long-running prune never holds one lock (or one transaction) across
 * every affected user, and can never race a push for the same user.
 */
@Service
class TombstoneRetentionService(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val advisoryLock: AdvisoryLock,
    private val transactionTemplate: TransactionTemplate,
    @Value("\${gymbuddy.sync.tombstone-retention-days:90}") private val retentionDays: Long,
) {
    private val log = LoggerFactory.getLogger(TombstoneRetentionService::class.java)

    companion object {
        private val TOMBSTONED_TABLES = listOf(
            TombstonedTable(EntityType.WORKOUT_SESSION, "workout_sessions", "id"),
            TombstonedTable(EntityType.EXERCISE, "exercises", "id"),
            TombstonedTable(EntityType.WORKOUT_TEMPLATE, "workout_templates", "id"),
        )
    }

    fun pruneExpiredTombstones(): RetentionSummary {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays)
        val userIds = findUsersWithExpiredTombstones(cutoff)

        var entitiesDeleted = 0
        var changeLogRowsDeleted = 0
        for (uid in userIds) {
            transactionTemplate.execute {
                advisoryLock.acquireForUser(uid)
                val (entities, changeLogRows) = pruneForUser(uid, cutoff)
                entitiesDeleted += entities
                changeLogRowsDeleted += changeLogRows
            }
        }

        val summary = RetentionSummary(userIds.size, entitiesDeleted, changeLogRowsDeleted)
        if (summary.usersProcessed > 0) {
            log.info(
                "tombstone retention pass complete: usersProcessed={} entitiesDeleted={} changeLogRowsDeleted={}",
                summary.usersProcessed, summary.entitiesDeleted, summary.changeLogRowsDeleted,
            )
        }
        return summary
    }

    private fun findUsersWithExpiredTombstones(cutoff: Long): List<String> {
        val sql = TOMBSTONED_TABLES.joinToString(separator = " UNION ") { t ->
            "SELECT owner_id FROM ${t.tableName} WHERE deleted_at IS NOT NULL AND deleted_at < :cutoff"
        }
        return jdbcTemplate.query(sql, mapOf("cutoff" to cutoff)) { rs, _ -> rs.getString(1) }
    }

    /** Runs inside the caller's transaction, after the advisory lock for uid is held. Returns (entities deleted, change_log rows deleted). */
    private fun pruneForUser(uid: String, cutoff: Long): Pair<Int, Int> {
        var entitiesDeleted = 0
        var changeLogRowsDeleted = 0
        var maxDeletedSeq: Long? = null

        for (table in TOMBSTONED_TABLES) {
            val idsSql = """
                SELECT ${table.idColumn} FROM ${table.tableName}
                WHERE owner_id = :uid AND deleted_at IS NOT NULL AND deleted_at < :cutoff
            """.trimIndent()
            val ids = jdbcTemplate.query(idsSql, mapOf("uid" to uid, "cutoff" to cutoff)) { rs, _ -> rs.getString(1) }
            if (ids.isEmpty()) continue

            // Captured before the change_log rows themselves are deleted below.
            val maxSeqSql = """
                SELECT MAX(seq) AS value FROM change_log
                WHERE user_id = :uid AND entity_type = :entityType AND entity_id IN (:ids)
            """.trimIndent()
            val maxSeqForTable = jdbcTemplate.query(
                maxSeqSql,
                mapOf("uid" to uid, "entityType" to table.entityType.name, "ids" to ids),
            ) { rs, _ -> rs.getLong("value").takeUnless { rs.wasNull() } }.firstOrNull()
            if (maxSeqForTable != null) {
                maxDeletedSeq = maxOf(maxDeletedSeq ?: maxSeqForTable, maxSeqForTable)
            }

            val deleteLogSql = """
                DELETE FROM change_log WHERE user_id = :uid AND entity_type = :entityType AND entity_id IN (:ids)
            """.trimIndent()
            changeLogRowsDeleted += jdbcTemplate.update(
                deleteLogSql,
                mapOf("uid" to uid, "entityType" to table.entityType.name, "ids" to ids),
            )

            // FK CASCADE removes aggregate children (performed_exercises/
            // workout_sets/template_exercises) - see V1's file header.
            val deleteEntitySql = "DELETE FROM ${table.tableName} WHERE ${table.idColumn}::text IN (:ids)"
            entitiesDeleted += jdbcTemplate.update(deleteEntitySql, mapOf("ids" to ids))
        }

        maxDeletedSeq?.let { advanceWatermark(uid, it) }
        return entitiesDeleted to changeLogRowsDeleted
    }

    private fun advanceWatermark(uid: String, floorSeq: Long) {
        val sql = """
            INSERT INTO sync_retention_watermark (user_id, retention_floor_seq, updated_at)
            VALUES (:uid, :floorSeq, :now)
            ON CONFLICT (user_id) DO UPDATE SET
                retention_floor_seq = GREATEST(sync_retention_watermark.retention_floor_seq, EXCLUDED.retention_floor_seq),
                updated_at = EXCLUDED.updated_at
        """.trimIndent()
        jdbcTemplate.update(sql, mapOf("uid" to uid, "floorSeq" to floorSeq, "now" to System.currentTimeMillis()))
    }
}
