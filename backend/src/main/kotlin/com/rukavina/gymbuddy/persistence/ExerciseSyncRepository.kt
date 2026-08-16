package com.rukavina.gymbuddy.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.rukavina.gymbuddy.api.dto.ExerciseSyncDto
import com.rukavina.gymbuddy.domain.DifficultyLevel
import com.rukavina.gymbuddy.domain.EntitySource
import com.rukavina.gymbuddy.domain.Equipment
import com.rukavina.gymbuddy.domain.ExerciseCategory
import com.rukavina.gymbuddy.domain.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.ExerciseType
import com.rukavina.gymbuddy.domain.MuscleGroup
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

/**
 * Write path for custom (user-owned) Exercise rows. Flat entity, no
 * aggregate children - unlike WorkoutSession/WorkoutTemplate there is
 * no child tree to replace.
 */
@Repository
class ExerciseSyncRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper,
) {

    fun find(id: String): StoredEntity<ExerciseSyncDto>? {
        val sql = """
            SELECT id, name, primary_muscles, secondary_muscles, description, instructions, tips, difficulty,
                   equipment_needed, category, exercise_type, tracking_type, video_url, thumbnail_url, source,
                   owner_id, derived_from_id, deprecated, updated_at, revision, deleted_at
            FROM exercises WHERE id = :id::uuid
        """.trimIndent()
        return jdbcTemplate.query(sql, MapSqlParameterSource("id", id)) { rs, _ -> mapRow(rs) }.firstOrNull()
    }

    /** deletedAt is the resolved value to persist: the caller has already turned "client asked to delete" into the server's own timestamp, or null. */
    fun insert(uid: String, dto: ExerciseSyncDto, now: Long, deletedAt: Long?) {
        val sql = """
            INSERT INTO exercises
                (id, name, primary_muscles, secondary_muscles, description, instructions, tips, difficulty,
                 equipment_needed, category, exercise_type, tracking_type, video_url, thumbnail_url, source,
                 owner_id, derived_from_id, deprecated, updated_at, revision, deleted_at)
            VALUES
                (:id::uuid, :name, :primaryMuscles::jsonb, :secondaryMuscles::jsonb, :description, :instructions::jsonb,
                 :tips::jsonb, :difficulty, :equipmentNeeded::jsonb, :category, :exerciseType, :trackingType,
                 :videoUrl, :thumbnailUrl, :source, :ownerId, :derivedFromId::uuid, :deprecated, :now, 1, :deletedAt)
        """.trimIndent()
        jdbcTemplate.update(sql, params(dto).addValue("ownerId", uid).addValue("now", now).addValue("deletedAt", deletedAt))
    }

    fun update(dto: ExerciseSyncDto, newRevision: Int, now: Long, deletedAt: Long?) {
        val sql = """
            UPDATE exercises
            SET name = :name, primary_muscles = :primaryMuscles::jsonb, secondary_muscles = :secondaryMuscles::jsonb,
                description = :description, instructions = :instructions::jsonb, tips = :tips::jsonb,
                difficulty = :difficulty, equipment_needed = :equipmentNeeded::jsonb, category = :category,
                exercise_type = :exerciseType, tracking_type = :trackingType, video_url = :videoUrl,
                thumbnail_url = :thumbnailUrl, source = :source, derived_from_id = :derivedFromId::uuid,
                deprecated = :deprecated, updated_at = :now, revision = :newRevision, deleted_at = :deletedAt
            WHERE id = :id::uuid
        """.trimIndent()
        jdbcTemplate.update(
            sql,
            params(dto).addValue("now", now).addValue("newRevision", newRevision).addValue("deletedAt", deletedAt),
        )
    }

    private fun params(dto: ExerciseSyncDto): MapSqlParameterSource =
        MapSqlParameterSource()
            .addValue("id", dto.id)
            .addValue("name", dto.name)
            .addValue("primaryMuscles", objectMapper.writeValueAsString(dto.primaryMuscles.map { it.name }))
            .addValue("secondaryMuscles", objectMapper.writeValueAsString(dto.secondaryMuscles.map { it.name }))
            .addValue("description", dto.description)
            .addValue("instructions", objectMapper.writeValueAsString(dto.instructions))
            .addValue("tips", objectMapper.writeValueAsString(dto.tips))
            .addValue("difficulty", dto.difficulty.name)
            .addValue("equipmentNeeded", objectMapper.writeValueAsString(dto.equipmentNeeded.map { it.name }))
            .addValue("category", dto.category.name)
            .addValue("exerciseType", dto.exerciseType.name)
            .addValue("trackingType", dto.trackingType.name)
            .addValue("videoUrl", dto.videoUrl)
            .addValue("thumbnailUrl", dto.thumbnailUrl)
            .addValue("source", dto.source.name)
            .addValue("derivedFromId", dto.derivedFromId)
            .addValue("deprecated", dto.deprecated)

    private fun mapRow(rs: ResultSet): StoredEntity<ExerciseSyncDto> {
        val dto = ExerciseSyncDto(
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
            derivedFromId = rs.getString("derived_from_id"),
            deprecated = rs.getBoolean("deprecated"),
            revision = rs.getInt("revision"),
            deletedAt = rs.getNullableLong("deleted_at"),
        )
        val ownerId = rs.getString("owner_id") ?: ""
        return StoredEntity(ownerId, rs.getInt("revision"), rs.getLong("updated_at"), dto)
    }
}
