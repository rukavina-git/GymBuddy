package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.domain.EntityType
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

data class FullSyncPage(val entities: List<EntityRef>, val hasMore: Boolean)

/**
 * Entity discovery for a full initial sync (point 4) - the counterpart
 * to ChangeLogReader for the one case where reading change_log would be
 * wrong: an entity whose only change_log row predates the log, or has
 * since been pruned by retention (Group H), must still appear in a full
 * sync, so this reads current state directly from the six owned-entity
 * tables instead. Written once here rather than duplicated per type,
 * since the ordering and page budget span all types together - see
 * fetchPage's ORDER BY.
 */
@Component
class FullSyncEntityDiscovery(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    /**
     * Deterministic order across all six tables combined: entity type
     * name, then id (point 4). offset/limit apply to that single
     * combined sequence, not per type - a user with 300 exercises and 0
     * sessions fills whole pages with EXERCISE before a WORKOUT_SESSION
     * (alphabetically later) is ever seen.
     *
     * Same fetch-limit+1 trim used by ChangeLogReader.fetchDeltaPage, so
     * hasMore is known without a second COUNT query.
     */
    fun fetchPage(userId: String, offset: Long, limit: Int): FullSyncPage {
        val sql = """
            SELECT entity_type, entity_id FROM (
                SELECT 'WORKOUT_SESSION' AS entity_type, id::text AS entity_id FROM workout_sessions WHERE owner_id = :userId
                UNION ALL
                SELECT 'EXERCISE' AS entity_type, id::text AS entity_id FROM exercises WHERE owner_id = :userId
                UNION ALL
                SELECT 'WORKOUT_TEMPLATE' AS entity_type, id::text AS entity_id FROM workout_templates WHERE owner_id = :userId
                UNION ALL
                SELECT 'USER_EXERCISE_STATE' AS entity_type, exercise_id::text AS entity_id FROM user_exercise_state WHERE owner_id = :userId
                UNION ALL
                SELECT 'USER_TEMPLATE_STATE' AS entity_type, template_id::text AS entity_id FROM user_template_state WHERE owner_id = :userId
                UNION ALL
                SELECT 'USER_PROFILE' AS entity_type, uid AS entity_id FROM user_profile WHERE uid = :userId
            ) owned_entities
            ORDER BY entity_type, entity_id
            LIMIT :fetchLimit OFFSET :offset
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("fetchLimit", limit + 1)
            .addValue("offset", offset)
        val rows = jdbcTemplate.query(sql, params) { rs, _ ->
            EntityRef(EntityType.valueOf(rs.getString("entity_type")), rs.getString("entity_id"))
        }

        val hasMore = rows.size > limit
        val page = if (hasMore) rows.subList(0, limit) else rows
        return FullSyncPage(page, hasMore)
    }
}
