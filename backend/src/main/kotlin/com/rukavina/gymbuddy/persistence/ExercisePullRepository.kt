package com.rukavina.gymbuddy.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.rukavina.gymbuddy.api.dto.ExerciseDto
import com.rukavina.gymbuddy.domain.DifficultyLevel
import com.rukavina.gymbuddy.domain.EntitySource
import com.rukavina.gymbuddy.domain.Equipment
import com.rukavina.gymbuddy.domain.ExerciseCategory
import com.rukavina.gymbuddy.domain.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.ExerciseType
import com.rukavina.gymbuddy.domain.MuscleGroup
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

/**
 * Read path for pull: bulk-loads custom Exercise rows by id, reusing
 * ExerciseDto (ReferenceDtos.kt) directly - same response shape the
 * reference-library endpoints already use, just scoped to one owner's
 * ids instead of source = 'DEFAULT'.
 */
@Repository
class ExercisePullRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper,
) {

    fun findAllByIds(userId: String, ids: Collection<String>): List<ExerciseDto> {
        if (ids.isEmpty()) return emptyList()
        val sql = """
            SELECT id, name, primary_muscles, secondary_muscles, description, instructions, tips, difficulty,
                   equipment_needed, category, exercise_type, tracking_type, video_url, thumbnail_url, source,
                   owner_id, derived_from_id, deprecated, updated_at, deleted_at, revision
            FROM exercises
            WHERE owner_id = :userId AND id::text IN (:ids)
        """.trimIndent()
        return jdbcTemplate.query(sql, mapOf("userId" to userId, "ids" to ids)) { rs, _ -> mapRow(rs) }
    }

    private fun mapRow(rs: ResultSet): ExerciseDto = ExerciseDto(
        id = rs.getString("id"),
        name = rs.getString("name"),
        primaryMuscles = objectMapper.readStringArrayColumn(rs, "primary_muscles").map { MuscleGroup.valueOf(it) },
        secondaryMuscles = objectMapper.readStringArrayColumn(rs, "secondary_muscles").map { MuscleGroup.valueOf(it) },
        description = rs.getString("description"),
        instructions = objectMapper.readStringArrayColumn(rs, "instructions"),
        tips = objectMapper.readStringArrayColumn(rs, "tips"),
        difficulty = DifficultyLevel.valueOf(rs.getString("difficulty")),
        equipmentNeeded = objectMapper.readStringArrayColumn(rs, "equipment_needed").map { Equipment.valueOf(it) },
        category = ExerciseCategory.valueOf(rs.getString("category")),
        exerciseType = ExerciseType.valueOf(rs.getString("exercise_type")),
        trackingType = ExerciseTrackingType.valueOf(rs.getString("tracking_type")),
        videoUrl = rs.getString("video_url"),
        thumbnailUrl = rs.getString("thumbnail_url"),
        source = EntitySource.valueOf(rs.getString("source")),
        ownerId = rs.getString("owner_id"),
        derivedFromId = rs.getString("derived_from_id"),
        deprecated = rs.getBoolean("deprecated"),
        updatedAt = rs.getLong("updated_at"),
        deletedAt = rs.getNullableLong("deleted_at"),
        revision = rs.getInt("revision"),
    )
}
