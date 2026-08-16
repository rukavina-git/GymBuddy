package com.rukavina.gymbuddy.sync

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service

/** One violated invariant. `rule` is stable and greppable; `detail` carries enough context to investigate without another query. */
data class InvariantViolation(val rule: String, val detail: String)

/**
 * Cross-checks change_log against the tables it's supposed to describe.
 * Not wired to an endpoint yet (see Group F's design notes, point 11) -
 * a plain service method so it can be called from a test today and from
 * an admin endpoint or a scheduled job later without moving this logic.
 *
 * Four rules:
 *   1. every change_log row references an entity that still exists
 *   2. every non-deleted, ever-synced (revision >= 1) user-owned entity
 *      has at least one change_log row
 *   3. no orphaned aggregate child (performed_exercises/workout_sets/
 *      template_exercises whose parent no longer exists) - already
 *      enforced by real FK CASCADE constraints (see V1's file header),
 *      so this is a defensive check, not the primary guard
 *   4. no entity sits at revision 0 while still having a change_log row
 *
 * revision >= 1 in rule 2 is deliberate, not source = 'CUSTOM': a
 * user_profile row is created by just-in-time provisioning
 * (UserProvisioningService.ensureProfile) at revision 0 before it is
 * ever pushed, and DEFAULT exercises/templates are seeded at revision 0
 * and never mutated by push - both are legitimately absent from
 * change_log, and both are also excluded by "revision >= 1" without
 * needing a per-table ownership/source special case. Rule 4 is this
 * rule's converse: together they enforce revision == 0 iff no
 * change_log row exists yet for that entity.
 */
@Service
class SyncInvariantChecker(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    fun check(): List<InvariantViolation> =
        changeLogRowsReferenceExistingEntities() +
            everSyncedEntitiesHaveAChangeLogRow() +
            noOrphanedAggregateChildren() +
            noRevisionZeroEntityHasAChangeLogRow()

    /**
     * Written as a UNION ALL of one simple, single-conjunct-guarded
     * query per entity type - deliberately not one query with
     * `type-tag AND EXISTS(...::uuid cast...)` OR'd together. Postgres
     * documents that it does not guarantee left-to-right short-circuit
     * evaluation of AND/OR subexpressions; a USER_PROFILE row's
     * entity_id (a Firebase uid, not a UUID - see V3's migration
     * comment) reaching the ::uuid cast in a WORKOUT_SESSION branch
     * would throw "invalid input syntax for type uuid" and fail the
     * whole check. Each branch here is its own straightforward
     * single-table filtered scan, which Postgres evaluates the way it
     * looks.
     */
    private fun changeLogRowsReferenceExistingEntities(): List<InvariantViolation> {
        val sql = """
            SELECT cl.seq, cl.entity_type, cl.entity_id FROM change_log cl
            WHERE cl.entity_type = 'WORKOUT_SESSION'
              AND NOT EXISTS (SELECT 1 FROM workout_sessions s WHERE s.id = cl.entity_id::uuid)

            UNION ALL
            SELECT cl.seq, cl.entity_type, cl.entity_id FROM change_log cl
            WHERE cl.entity_type = 'EXERCISE'
              AND NOT EXISTS (SELECT 1 FROM exercises e WHERE e.id = cl.entity_id::uuid)

            UNION ALL
            SELECT cl.seq, cl.entity_type, cl.entity_id FROM change_log cl
            WHERE cl.entity_type = 'WORKOUT_TEMPLATE'
              AND NOT EXISTS (SELECT 1 FROM workout_templates t WHERE t.id = cl.entity_id::uuid)

            UNION ALL
            SELECT cl.seq, cl.entity_type, cl.entity_id FROM change_log cl
            WHERE cl.entity_type = 'USER_EXERCISE_STATE'
              AND NOT EXISTS (SELECT 1 FROM user_exercise_state ues WHERE ues.owner_id = cl.user_id AND ues.exercise_id = cl.entity_id::uuid)

            UNION ALL
            SELECT cl.seq, cl.entity_type, cl.entity_id FROM change_log cl
            WHERE cl.entity_type = 'USER_TEMPLATE_STATE'
              AND NOT EXISTS (SELECT 1 FROM user_template_state uts WHERE uts.owner_id = cl.user_id AND uts.template_id = cl.entity_id::uuid)

            UNION ALL
            SELECT cl.seq, cl.entity_type, cl.entity_id FROM change_log cl
            WHERE cl.entity_type = 'USER_PROFILE'
              AND NOT EXISTS (SELECT 1 FROM user_profile p WHERE p.uid = cl.user_id)
        """.trimIndent()
        return jdbcTemplate.query(sql, emptyMap<String, Any>()) { rs, _ ->
            InvariantViolation(
                rule = "change_log_row_references_existing_entity",
                detail = "change_log.seq=${rs.getLong("seq")} entityType=${rs.getString("entity_type")} entityId=${rs.getString("entity_id")} has no matching entity row",
            )
        }
    }

    private fun everSyncedEntitiesHaveAChangeLogRow(): List<InvariantViolation> {
        val sql = """
            SELECT 'WORKOUT_SESSION' AS entity_type, id::text AS entity_id FROM workout_sessions
            WHERE deleted_at IS NULL AND revision >= 1
              AND NOT EXISTS (SELECT 1 FROM change_log cl WHERE cl.entity_type = 'WORKOUT_SESSION' AND cl.entity_id = workout_sessions.id::text)

            UNION ALL
            SELECT 'EXERCISE', id::text FROM exercises
            WHERE deleted_at IS NULL AND revision >= 1
              AND NOT EXISTS (SELECT 1 FROM change_log cl WHERE cl.entity_type = 'EXERCISE' AND cl.entity_id = exercises.id::text)

            UNION ALL
            SELECT 'WORKOUT_TEMPLATE', id::text FROM workout_templates
            WHERE deleted_at IS NULL AND revision >= 1
              AND NOT EXISTS (SELECT 1 FROM change_log cl WHERE cl.entity_type = 'WORKOUT_TEMPLATE' AND cl.entity_id = workout_templates.id::text)

            UNION ALL
            SELECT 'USER_EXERCISE_STATE', exercise_id::text FROM user_exercise_state
            WHERE revision >= 1
              AND NOT EXISTS (
                SELECT 1 FROM change_log cl
                WHERE cl.entity_type = 'USER_EXERCISE_STATE' AND cl.user_id = user_exercise_state.owner_id
                  AND cl.entity_id = user_exercise_state.exercise_id::text
              )

            UNION ALL
            SELECT 'USER_TEMPLATE_STATE', template_id::text FROM user_template_state
            WHERE revision >= 1
              AND NOT EXISTS (
                SELECT 1 FROM change_log cl
                WHERE cl.entity_type = 'USER_TEMPLATE_STATE' AND cl.user_id = user_template_state.owner_id
                  AND cl.entity_id = user_template_state.template_id::text
              )

            UNION ALL
            SELECT 'USER_PROFILE', uid FROM user_profile
            WHERE deleted_at IS NULL AND revision >= 1
              AND NOT EXISTS (SELECT 1 FROM change_log cl WHERE cl.entity_type = 'USER_PROFILE' AND cl.entity_id = user_profile.uid)
        """.trimIndent()
        return jdbcTemplate.query(sql, emptyMap<String, Any>()) { rs, _ ->
            InvariantViolation(
                rule = "synced_entity_has_change_log_row",
                detail = "entityType=${rs.getString("entity_type")} entityId=${rs.getString("entity_id")} has revision >= 1 but no change_log row",
            )
        }
    }

    private fun noOrphanedAggregateChildren(): List<InvariantViolation> {
        val sql = """
            SELECT 'performed_exercises' AS table_name, pe.id::text AS id
            FROM performed_exercises pe
            WHERE NOT EXISTS (SELECT 1 FROM workout_sessions s WHERE s.id = pe.workout_session_id)

            UNION ALL
            SELECT 'workout_sets', ws.id::text
            FROM workout_sets ws
            WHERE NOT EXISTS (SELECT 1 FROM performed_exercises pe WHERE pe.id = ws.performed_exercise_id)

            UNION ALL
            SELECT 'template_exercises', te.id::text
            FROM template_exercises te
            WHERE NOT EXISTS (SELECT 1 FROM workout_templates t WHERE t.id = te.template_id)
        """.trimIndent()
        return jdbcTemplate.query(sql, emptyMap<String, Any>()) { rs, _ ->
            InvariantViolation(
                rule = "no_orphaned_aggregate_child",
                detail = "${rs.getString("table_name")}.id=${rs.getString("id")} has no parent row",
            )
        }
    }

    private fun noRevisionZeroEntityHasAChangeLogRow(): List<InvariantViolation> {
        val sql = """
            SELECT 'WORKOUT_SESSION' AS entity_type, id::text AS entity_id FROM workout_sessions
            WHERE revision = 0 AND EXISTS (SELECT 1 FROM change_log cl WHERE cl.entity_type = 'WORKOUT_SESSION' AND cl.entity_id = workout_sessions.id::text)

            UNION ALL
            SELECT 'EXERCISE', id::text FROM exercises
            WHERE revision = 0 AND EXISTS (SELECT 1 FROM change_log cl WHERE cl.entity_type = 'EXERCISE' AND cl.entity_id = exercises.id::text)

            UNION ALL
            SELECT 'WORKOUT_TEMPLATE', id::text FROM workout_templates
            WHERE revision = 0 AND EXISTS (SELECT 1 FROM change_log cl WHERE cl.entity_type = 'WORKOUT_TEMPLATE' AND cl.entity_id = workout_templates.id::text)

            UNION ALL
            SELECT 'USER_EXERCISE_STATE', exercise_id::text FROM user_exercise_state
            WHERE revision = 0 AND EXISTS (
                SELECT 1 FROM change_log cl
                WHERE cl.entity_type = 'USER_EXERCISE_STATE' AND cl.user_id = user_exercise_state.owner_id
                  AND cl.entity_id = user_exercise_state.exercise_id::text
            )

            UNION ALL
            SELECT 'USER_TEMPLATE_STATE', template_id::text FROM user_template_state
            WHERE revision = 0 AND EXISTS (
                SELECT 1 FROM change_log cl
                WHERE cl.entity_type = 'USER_TEMPLATE_STATE' AND cl.user_id = user_template_state.owner_id
                  AND cl.entity_id = user_template_state.template_id::text
            )

            UNION ALL
            SELECT 'USER_PROFILE', uid FROM user_profile
            WHERE revision = 0 AND EXISTS (SELECT 1 FROM change_log cl WHERE cl.entity_type = 'USER_PROFILE' AND cl.entity_id = user_profile.uid)
        """.trimIndent()
        return jdbcTemplate.query(sql, emptyMap<String, Any>()) { rs, _ ->
            InvariantViolation(
                rule = "no_revision_zero_entity_has_change_log_row",
                detail = "entityType=${rs.getString("entity_type")} entityId=${rs.getString("entity_id")} is at revision 0 but has a change_log row",
            )
        }
    }
}
