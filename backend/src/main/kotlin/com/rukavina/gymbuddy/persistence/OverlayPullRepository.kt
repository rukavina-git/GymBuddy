package com.rukavina.gymbuddy.persistence

import com.rukavina.gymbuddy.api.dto.UserExerciseStateDto
import com.rukavina.gymbuddy.api.dto.UserTemplateStateDto
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

/** Read path for pull: bulk-loads both overlay tables by id, response-shaped (UserExerciseStateDto/UserTemplateStateDto). */
@Repository
class OverlayPullRepository(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    fun findAllExerciseStatesByIds(userId: String, exerciseIds: Collection<String>): List<UserExerciseStateDto> {
        if (exerciseIds.isEmpty()) return emptyList()
        val sql = """
            SELECT exercise_id, is_hidden, is_favorite, note, default_rest_seconds, updated_at, revision
            FROM user_exercise_state
            WHERE owner_id = :userId AND exercise_id::text IN (:exerciseIds)
        """.trimIndent()
        return jdbcTemplate.query(sql, mapOf("userId" to userId, "exerciseIds" to exerciseIds)) { rs, _ -> mapExerciseStateRow(rs) }
    }

    fun findAllTemplateStatesByIds(userId: String, templateIds: Collection<String>): List<UserTemplateStateDto> {
        if (templateIds.isEmpty()) return emptyList()
        val sql = """
            SELECT template_id, is_hidden, is_favorite, updated_at, revision
            FROM user_template_state
            WHERE owner_id = :userId AND template_id::text IN (:templateIds)
        """.trimIndent()
        return jdbcTemplate.query(sql, mapOf("userId" to userId, "templateIds" to templateIds)) { rs, _ -> mapTemplateStateRow(rs) }
    }

    private fun mapExerciseStateRow(rs: ResultSet): UserExerciseStateDto = UserExerciseStateDto(
        exerciseId = rs.getString("exercise_id"),
        isHidden = rs.getBoolean("is_hidden"),
        isFavorite = rs.getBoolean("is_favorite"),
        note = rs.getString("note"),
        defaultRestSeconds = rs.getNullableInt("default_rest_seconds"),
        updatedAt = rs.getLong("updated_at"),
        revision = rs.getInt("revision"),
    )

    private fun mapTemplateStateRow(rs: ResultSet): UserTemplateStateDto = UserTemplateStateDto(
        templateId = rs.getString("template_id"),
        isHidden = rs.getBoolean("is_hidden"),
        isFavorite = rs.getBoolean("is_favorite"),
        updatedAt = rs.getLong("updated_at"),
        revision = rs.getInt("revision"),
    )
}
