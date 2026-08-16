package com.rukavina.gymbuddy.persistence

import com.rukavina.gymbuddy.api.dto.UserExerciseStateSyncDto
import com.rukavina.gymbuddy.api.dto.UserTemplateStateSyncDto
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

/**
 * Write path for both overlay tables (user_exercise_state,
 * user_template_state) - upsert-only, sparse, no deletedAt, no
 * aggregate children. Their primary key is (owner_id, <ref>_id), so a
 * lookup scoped to the caller's own uid can never resolve to another
 * user's row - there is no ownership check to perform here, unlike the
 * other four sync services.
 */
@Repository
class OverlaySyncRepository(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    fun findExerciseState(uid: String, exerciseId: String): StoredEntity<UserExerciseStateSyncDto>? {
        val sql = """
            SELECT is_hidden, is_favorite, note, default_rest_seconds, updated_at, revision
            FROM user_exercise_state WHERE owner_id = :uid AND exercise_id = :exerciseId::uuid
        """.trimIndent()
        val params = MapSqlParameterSource().addValue("uid", uid).addValue("exerciseId", exerciseId)
        return jdbcTemplate.query(sql, params) { rs, _ -> mapExerciseStateRow(uid, exerciseId, rs) }.firstOrNull()
    }

    fun insertExerciseState(uid: String, dto: UserExerciseStateSyncDto, now: Long) {
        val sql = """
            INSERT INTO user_exercise_state (owner_id, exercise_id, is_hidden, is_favorite, note, default_rest_seconds, updated_at, revision)
            VALUES (:uid, :exerciseId::uuid, :isHidden, :isFavorite, :note, :defaultRestSeconds, :now, 1)
        """.trimIndent()
        jdbcTemplate.update(sql, exerciseStateParams(uid, dto, now))
    }

    fun updateExerciseState(uid: String, dto: UserExerciseStateSyncDto, newRevision: Int, now: Long) {
        val sql = """
            UPDATE user_exercise_state
            SET is_hidden = :isHidden, is_favorite = :isFavorite, note = :note,
                default_rest_seconds = :defaultRestSeconds, updated_at = :now, revision = :newRevision
            WHERE owner_id = :uid AND exercise_id = :exerciseId::uuid
        """.trimIndent()
        jdbcTemplate.update(sql, exerciseStateParams(uid, dto, now).addValue("newRevision", newRevision))
    }

    private fun exerciseStateParams(uid: String, dto: UserExerciseStateSyncDto, now: Long): MapSqlParameterSource =
        MapSqlParameterSource()
            .addValue("uid", uid)
            .addValue("exerciseId", dto.exerciseId)
            .addValue("isHidden", dto.isHidden)
            .addValue("isFavorite", dto.isFavorite)
            .addValue("note", dto.note)
            .addValue("defaultRestSeconds", dto.defaultRestSeconds)
            .addValue("now", now)

    private fun mapExerciseStateRow(uid: String, exerciseId: String, rs: ResultSet): StoredEntity<UserExerciseStateSyncDto> {
        val dto = UserExerciseStateSyncDto(
            exerciseId = exerciseId,
            isHidden = rs.getBoolean("is_hidden"),
            isFavorite = rs.getBoolean("is_favorite"),
            note = rs.getString("note"),
            defaultRestSeconds = rs.getNullableInt("default_rest_seconds"),
            revision = rs.getInt("revision"),
        )
        return StoredEntity(uid, rs.getInt("revision"), rs.getLong("updated_at"), dto)
    }

    fun findTemplateState(uid: String, templateId: String): StoredEntity<UserTemplateStateSyncDto>? {
        val sql = """
            SELECT is_hidden, is_favorite, updated_at, revision
            FROM user_template_state WHERE owner_id = :uid AND template_id = :templateId::uuid
        """.trimIndent()
        val params = MapSqlParameterSource().addValue("uid", uid).addValue("templateId", templateId)
        return jdbcTemplate.query(sql, params) { rs, _ -> mapTemplateStateRow(uid, templateId, rs) }.firstOrNull()
    }

    fun insertTemplateState(uid: String, dto: UserTemplateStateSyncDto, now: Long) {
        val sql = """
            INSERT INTO user_template_state (owner_id, template_id, is_hidden, is_favorite, updated_at, revision)
            VALUES (:uid, :templateId::uuid, :isHidden, :isFavorite, :now, 1)
        """.trimIndent()
        jdbcTemplate.update(sql, templateStateParams(uid, dto, now))
    }

    fun updateTemplateState(uid: String, dto: UserTemplateStateSyncDto, newRevision: Int, now: Long) {
        val sql = """
            UPDATE user_template_state
            SET is_hidden = :isHidden, is_favorite = :isFavorite, updated_at = :now, revision = :newRevision
            WHERE owner_id = :uid AND template_id = :templateId::uuid
        """.trimIndent()
        jdbcTemplate.update(sql, templateStateParams(uid, dto, now).addValue("newRevision", newRevision))
    }

    private fun templateStateParams(uid: String, dto: UserTemplateStateSyncDto, now: Long): MapSqlParameterSource =
        MapSqlParameterSource()
            .addValue("uid", uid)
            .addValue("templateId", dto.templateId)
            .addValue("isHidden", dto.isHidden)
            .addValue("isFavorite", dto.isFavorite)
            .addValue("now", now)

    private fun mapTemplateStateRow(uid: String, templateId: String, rs: ResultSet): StoredEntity<UserTemplateStateSyncDto> {
        val dto = UserTemplateStateSyncDto(
            templateId = templateId,
            isHidden = rs.getBoolean("is_hidden"),
            isFavorite = rs.getBoolean("is_favorite"),
            revision = rs.getInt("revision"),
        )
        return StoredEntity(uid, rs.getInt("revision"), rs.getLong("updated_at"), dto)
    }
}
