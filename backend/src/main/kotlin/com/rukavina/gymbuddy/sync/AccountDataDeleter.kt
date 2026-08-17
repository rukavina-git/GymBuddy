package com.rukavina.gymbuddy.sync

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

/**
 * The actual row-deletion for account deletion, split out from
 * AccountDeletionService as its own bean so a test can replace it with
 * a throwing mock and verify FirebaseUserDeleter is never reached when
 * it does (see AccountDeletionTest) - the same reasoning as Group F's
 * per-type sync repositories being separately mockable from their
 * services.
 *
 * Every user-owned table, one DELETE each. FK CASCADE handles the
 * aggregate children (performed_exercises/workout_sets under
 * workout_sessions, template_exercises under workout_templates) - see
 * V1's file header. No table here has a real FK to another in this
 * list, so statement order doesn't matter for referential integrity;
 * the caller (AccountDeletionService) is responsible for running all of
 * this in one transaction so it's all-or-nothing regardless.
 */
@Component
class AccountDataDeleter(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    fun deleteAllDataFor(uid: String) {
        val params = MapSqlParameterSource("uid", uid)
        jdbcTemplate.update("DELETE FROM workout_sessions WHERE owner_id = :uid", params)
        jdbcTemplate.update("DELETE FROM exercises WHERE owner_id = :uid", params)
        jdbcTemplate.update("DELETE FROM workout_templates WHERE owner_id = :uid", params)
        jdbcTemplate.update("DELETE FROM user_exercise_state WHERE owner_id = :uid", params)
        jdbcTemplate.update("DELETE FROM user_template_state WHERE owner_id = :uid", params)
        jdbcTemplate.update("DELETE FROM user_profile WHERE uid = :uid", params)
        jdbcTemplate.update("DELETE FROM change_log WHERE user_id = :uid", params)
        jdbcTemplate.update("DELETE FROM sync_retention_watermark WHERE user_id = :uid", params)
    }
}
