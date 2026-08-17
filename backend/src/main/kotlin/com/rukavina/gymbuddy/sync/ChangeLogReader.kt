package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.domain.EntityType
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

/** One entity referenced by the change log - which table/id to go re-read current state from. */
data class EntityRef(val entityType: EntityType, val entityId: String)

data class DeltaPage(val entities: List<EntityRef>, val nextCursorSeq: Long, val hasMore: Boolean)

/**
 * The one place that reads change_log for pull - the counterpart to
 * ChangeLogWriter (Group F), which is the one place that writes it.
 * Every per-type pull service is handed entity ids by
 * SyncPullOrchestrator, which gets them from here; none of them query
 * change_log directly.
 */
@Component
class ChangeLogReader(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    /**
     * Rows with seq > afterSeq, oldest first, for this user only (point
     * 6: ownership). Fetches limit+1 change_log rows rather than a
     * separate COUNT query to decide hasMore - the classic
     * fetch-one-extra trick. The limit bounds change_log ROWS, not
     * entities: dedup happens after the fetch/trim, so the returned
     * entity list can be shorter than limit even when hasMore is true
     * (point 3) - five rows for one repeatedly-edited entity collapse to
     * one EntityRef, still counting as five rows against the limit.
     *
     * nextCursorSeq is always the max seq among the rows actually
     * included in this page (never the trimmed-off extra row, which
     * belongs to the next page) - "highest seq among them" from the
     * design notes. When nothing new exists, returns the same afterSeq
     * back unchanged with hasMore=false, so a client polling an
     * up-to-date cursor gets a stable, empty result.
     */
    fun fetchDeltaPage(userId: String, afterSeq: Long, limit: Int): DeltaPage {
        val sql = """
            SELECT seq, entity_type, entity_id FROM change_log
            WHERE user_id = :userId AND seq > :afterSeq
            ORDER BY seq
            LIMIT :fetchLimit
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("afterSeq", afterSeq)
            .addValue("fetchLimit", limit + 1)
        val rows = jdbcTemplate.query(sql, params) { rs, _ ->
            Triple(rs.getLong("seq"), rs.getString("entity_type"), rs.getString("entity_id"))
        }

        if (rows.isEmpty()) return DeltaPage(emptyList(), afterSeq, hasMore = false)

        val hasMore = rows.size > limit
        val page = if (hasMore) rows.subList(0, limit) else rows
        val nextCursorSeq = page.last().first
        val entities = page
            .map { (_, entityType, entityId) -> EntityRef(EntityType.valueOf(entityType), entityId) }
            .distinct()

        return DeltaPage(entities, nextCursorSeq, hasMore)
    }

    /**
     * Point 5 (Group G), closed properly by Group H's watermark: a
     * cursor is expired when it predates retention_floor_seq -
     * sync_retention_watermark's record of the highest change_log.seq
     * TombstoneRetentionService has ever deleted for this user (see
     * that table's migration comment and TombstoneRetentionService).
     * No watermark row means retention has never touched this user's
     * data, so nothing can be expired for them.
     *
     * This used to infer "oldest retained" from MIN(seq) over
     * change_log's current contents, which was an unsound proxy: once
     * pruning is real, removing even an already-seen row can make
     * MIN(seq) jump past a fully-caught-up cursor and flag it expired
     * for no reason - not a corner case for this app, since a user who
     * doesn't log a workout for 90 days is routine, not exceptional.
     * The stored watermark only ever advances past a seq the retention
     * job has actually deleted, so that false positive is gone: a
     * cursor sitting anywhere at or after the watermark is guaranteed
     * safe, regardless of what gaps exist in what currently remains.
     *
     * cursorSeq <= 0 is still exempted explicitly, defensively: a
     * legitimately-issued Cursor.Delta.seq of exactly 0 is
     * SyncPullOrchestrator's "nothing synced yet" full-sync terminal
     * cursor for a user with no change_log rows, which the missing-
     * watermark-row check above already handles correctly on its own
     * (retention has nothing to have pruned for such a user). A
     * negative cursorSeq can only reach this method via a malformed or
     * adversarial Cursor.Delta - CursorCodec's own encode() never
     * produces one, and genuine full-sync continuation is a distinct
     * Cursor.FullSync that never reaches isCursorExpired at all. The
     * guard just gives that case the same harmless "seq > N returns
     * everything" treatment as 0 instead of a spurious 410.
     */
    fun isCursorExpired(userId: String, cursorSeq: Long): Boolean {
        if (cursorSeq <= 0) return false
        val retentionFloorSeq = retentionFloorSeqForUser(userId) ?: return false
        return cursorSeq < retentionFloorSeq
    }

    /** The cursor a completed full sync hands back, so the client can move to ordinary delta pulls from here. Null if the user has no change_log rows yet. */
    fun currentMaxSeq(userId: String): Long? {
        val sql = "SELECT MAX(seq) AS value FROM change_log WHERE user_id = :userId"
        return jdbcTemplate.query(sql, MapSqlParameterSource("userId", userId)) { rs, _ ->
            rs.getLong("value").takeUnless { rs.wasNull() }
        }.firstOrNull()
    }

    private fun retentionFloorSeqForUser(userId: String): Long? {
        val sql = "SELECT retention_floor_seq FROM sync_retention_watermark WHERE user_id = :userId"
        return jdbcTemplate.query(sql, MapSqlParameterSource("userId", userId)) { rs, _ ->
            rs.getLong("retention_floor_seq")
        }.firstOrNull()
    }
}
