package com.rukavina.gymbuddy.persistence

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/** One feedback row as the admin page sees it. userId null = guest. */
data class FeedbackRow(
    val id: UUID,
    val userId: String?,
    val message: String,
    val status: String,
    val note: String?,
    val appVersion: String?,
    val osVersion: String?,
    val deviceModel: String?,
    val createdAt: Instant,
    val receivedAt: Instant,
    val updatedAt: Instant,
)

/** Keyset position in the admin list (newest first): the last row's receivedAt and id. */
data class FeedbackPosition(val receivedAt: Instant, val id: UUID)

/**
 * The feedback table (V9, V10). The app inserts; the admin page
 * (06d) lists and sets status/note; docker/server/feedback.sh reads.
 */
@Repository
class FeedbackRepository(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    /** Returns false when the id is already stored (a client retry), true when a row was added. */
    fun insertIfAbsent(
        id: UUID,
        userId: String?,
        message: String,
        appVersion: String?,
        osVersion: String?,
        deviceModel: String?,
        createdAt: Instant,
    ): Boolean {
        val params = MapSqlParameterSource()
            .addValue("id", id)
            .addValue("userId", userId)
            .addValue("message", message)
            .addValue("appVersion", appVersion)
            .addValue("osVersion", osVersion)
            .addValue("deviceModel", deviceModel)
            .addValue("createdAt", Timestamp.from(createdAt))
        return jdbcTemplate.update(
            """
            INSERT INTO feedback (id, user_id, message, app_version, os_version, device_model, created_at)
            VALUES (:id, :userId, :message, :appVersion, :osVersion, :deviceModel, :createdAt)
            ON CONFLICT (id) DO NOTHING
            """.trimIndent(),
            params,
        ) == 1
    }

    /** Newest first, strictly after [after]; [status] null = every status. */
    fun list(status: String?, after: FeedbackPosition?, limit: Int): List<FeedbackRow> {
        val params = MapSqlParameterSource()
            .addValue("status", status)
            .addValue("afterReceivedAt", after?.let { Timestamp.from(it.receivedAt) })
            .addValue("afterId", after?.id)
            .addValue("limit", limit)
        val where = buildList {
            if (status != null) add("status = :status")
            if (after != null) add("(received_at, id) < (:afterReceivedAt, :afterId)")
        }
        val whereSql = if (where.isEmpty()) "" else "WHERE " + where.joinToString(" AND ")
        return jdbcTemplate.query(
            "SELECT * FROM feedback $whereSql ORDER BY received_at DESC, id DESC LIMIT :limit",
            params,
        ) { rs, _ -> rs.toRow() }
    }

    /** Rows per status; statuses with none are absent. */
    fun countByStatus(): Map<String, Long> =
        jdbcTemplate.query("SELECT status, count(*) AS n FROM feedback GROUP BY status", MapSqlParameterSource()) { rs, _ ->
            rs.getString("status") to rs.getLong("n")
        }.toMap()

    /**
     * Sets status and/or note (null = leave as is; [clearNote] sets the
     * note to null) and bumps updated_at. Returns the row, or null for
     * an unknown id.
     */
    fun update(id: UUID, status: String?, note: String?, clearNote: Boolean): FeedbackRow? {
        val params = MapSqlParameterSource()
            .addValue("id", id)
            .addValue("status", status)
            .addValue("note", note)
            .addValue("clearNote", clearNote)
        return jdbcTemplate.query(
            """
            UPDATE feedback SET
                status = coalesce(CAST(:status AS TEXT), status),
                note = CASE WHEN :clearNote THEN NULL ELSE coalesce(CAST(:note AS TEXT), note) END,
                updated_at = now()
            WHERE id = :id
            RETURNING *
            """.trimIndent(),
            params,
        ) { rs, _ -> rs.toRow() }.singleOrNull()
    }

    /** Hard-deletes every row received before [cutoff], any status. Returns the count. */
    fun deleteReceivedBefore(cutoff: Instant): Int =
        jdbcTemplate.update(
            "DELETE FROM feedback WHERE received_at < :cutoff",
            MapSqlParameterSource("cutoff", Timestamp.from(cutoff)),
        )

    private fun ResultSet.toRow() = FeedbackRow(
        id = getObject("id", UUID::class.java),
        userId = getString("user_id"),
        message = getString("message"),
        status = getString("status"),
        note = getString("note"),
        appVersion = getString("app_version"),
        osVersion = getString("os_version"),
        deviceModel = getString("device_model"),
        createdAt = getTimestamp("created_at").toInstant(),
        receivedAt = getTimestamp("received_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}
