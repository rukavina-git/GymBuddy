package com.rukavina.gymbuddy.persistence

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/** The feedback table (V9). Insert only; the owner reads it with docker/server/feedback.sh. */
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
}
