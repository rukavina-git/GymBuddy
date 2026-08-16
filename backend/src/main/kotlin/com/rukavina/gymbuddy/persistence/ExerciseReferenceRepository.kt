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
 * The default (server-owned) exercise library only - GET
 * /v1/reference/exercises per api/openapi.yaml. Deprecated rows are
 * included deliberately (clients need them to resolve existing
 * references); this is not user data, so there is no owner parameter.
 * Custom, user-owned exercises are a Sync-group concern, not this one.
 */
@Repository
class ExerciseReferenceRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper,
) {

    fun findAllDefault(): List<ExerciseDto> {
        val sql = """
            SELECT id, name, primary_muscles, secondary_muscles, description, instructions, tips,
                   difficulty, equipment_needed, category, exercise_type, tracking_type, video_url,
                   thumbnail_url, source, owner_id, derived_from_id, deprecated, updated_at,
                   deleted_at, revision
            FROM exercises
            WHERE source = 'DEFAULT'
            ORDER BY name
        """.trimIndent()
        return jdbcTemplate.query(sql, emptyMap<String, Any>()) { rs, _ -> mapRow(rs) }
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
