package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.domain.EntityType
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

/** operation values recorded in change_log - see ChangeLogWriter's header. */
enum class ChangeOperation { UPSERT, DELETE }

/**
 * The one place that writes to change_log. Every per-type sync service
 * calls this - and only this - after a successful insert or update, in
 * the same transaction as the mutation; never conditionally, never
 * best-effort. Group G's pull cursor is a position in (user_id, seq)
 * order over this table, so a mutation applied without a matching row
 * here is invisible to every other device permanently.
 *
 * operation is DELETE when the applied push carried a non-null
 * deletedAt (see WorkoutSession/Exercise/WorkoutTemplate in
 * api/openapi.yaml), UPSERT otherwise. userExerciseState/
 * userTemplateState/userProfile never produce DELETE - they have no
 * client-facing delete path (see push endpoint's description).
 */
@Component
class ChangeLogWriter(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    fun record(userId: String, entityType: EntityType, entityId: String, operation: ChangeOperation = ChangeOperation.UPSERT) {
        val sql = """
            INSERT INTO change_log (user_id, entity_type, entity_id, operation)
            VALUES (:userId, :entityType, :entityId, :operation)
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("entityType", entityType.name)
            .addValue("entityId", entityId)
            .addValue("operation", operation.name)
        jdbcTemplate.update(sql, params)
    }
}
