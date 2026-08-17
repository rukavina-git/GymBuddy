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
     * Point 5: a cursor is expired when it predates the oldest
     * change_log row this user currently has - i.e. something between
     * the cursor's position and that oldest row was pruned (Group H,
     * not yet implemented) and can never be replayed. Deliberately
     * per-user, not a table-wide MIN(seq): seq is one global BIGSERIAL
     * shared by every user, so a per-user gap in the raw numbers is
     * normal (other users' activity fills it) and proves nothing on its
     * own - only a gap in THIS user's own oldest-surviving-row position
     * is meaningful. A user with no change_log rows at all has nothing
     * to be behind on, so no cursor can be expired for them.
     *
     * cursorSeq <= 0 is exempted unconditionally - not just an
     * optimisation. Every legitimately-issued Cursor.Delta.seq other
     * than 0 corresponds to an actual row this user once had (a delta
     * page's own max seq, or a completed full sync's currentMaxSeq via
     * ChangeLogReader.currentMaxSeq), so "MIN(seq for user) now exceeds
     * it" can only mean that specific row was pruned. The single
     * exception: SyncPullOrchestrator hands a user with zero change_log
     * rows the value 0 as their full-sync terminal cursor - not a real
     * row, just "nothing yet". The very first change that user EVER
     * makes necessarily gets a seq greater than 0 (BIGSERIAL starts at
     * 1), so comparing MIN(seq for user) against a 0 baseline would
     * always look like a gap - the user's oldest row is trivially
     * "later than" a cursor issued before they had any row at all.
     * That's not pruning, it's just their first activity; a plain
     * seq > 0 query already returns 100% of it, so there is nothing to
     * protect against here. (A negative cursorSeq can only reach this
     * method via a malformed/adversarial Cursor.Delta - CursorCodec's
     * own encode() never produces one, and a genuine full-sync
     * continuation is a distinct Cursor.FullSync that never reaches
     * isCursorExpired at all. The <= 0 guard, rather than == 0, just
     * gives that case the same harmless "seq > N returns everything"
     * treatment instead of a spurious 410.)
     *
     * TODO(Group H): this infers "oldest retained" from MIN(seq) over
     * change_log's current contents, which is only a proxy and is known
     * to over-trigger once real pruning exists. If retention removes
     * even an ALREADY-SEEN row - e.g. a long-dormant user's rows 5,6,7
     * age out, but they made one recent edit at row 8 that's still
     * within the window - MIN(seq for user) jumps to 8, and a fully
     * caught-up cursor of 7 reads as "7 < 8" and gets flagged expired,
     * even though nothing the client hasn't already seen was lost. This
     * is not a rare shape for this app specifically: a user who doesn't
     * log a workout for 90 days is normal, not exceptional, so this
     * would force routine unnecessary full resyncs, not just handle a
     * corner case. Group H (the retention job) must close this: add a
     * per-user watermark (a column, or a small table keyed on user_id)
     * recording the oldest seq retention still GUARANTEES is intact for
     * that user, written by the retention job at prune time - not
     * inferred here. isCursorExpired then becomes a direct comparison
     * against that stored watermark instead of MIN(seq) over whatever
     * happens to remain in change_log, and the false-positive above
     * goes away because the watermark only advances past a row the
     * pruning job is certain no still-valid cursor could still need.
     */
    fun isCursorExpired(userId: String, cursorSeq: Long): Boolean {
        if (cursorSeq <= 0) return false
        val oldestRetainedSeq = oldestSeqForUser(userId) ?: return false
        return cursorSeq < oldestRetainedSeq
    }

    /** The cursor a completed full sync hands back, so the client can move to ordinary delta pulls from here. Null if the user has no change_log rows yet. */
    fun currentMaxSeq(userId: String): Long? {
        val sql = "SELECT MAX(seq) AS value FROM change_log WHERE user_id = :userId"
        return jdbcTemplate.query(sql, MapSqlParameterSource("userId", userId)) { rs, _ ->
            rs.getLong("value").takeUnless { rs.wasNull() }
        }.firstOrNull()
    }

    private fun oldestSeqForUser(userId: String): Long? {
        val sql = "SELECT MIN(seq) AS value FROM change_log WHERE user_id = :userId"
        return jdbcTemplate.query(sql, MapSqlParameterSource("userId", userId)) { rs, _ ->
            rs.getLong("value").takeUnless { rs.wasNull() }
        }.firstOrNull()
    }
}
