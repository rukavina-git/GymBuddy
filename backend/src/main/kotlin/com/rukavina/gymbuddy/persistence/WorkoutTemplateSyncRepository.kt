package com.rukavina.gymbuddy.persistence

import com.rukavina.gymbuddy.api.dto.TemplateExerciseSyncDto
import com.rukavina.gymbuddy.api.dto.WorkoutTemplateSyncDto
import com.rukavina.gymbuddy.domain.EntitySource
import com.rukavina.gymbuddy.domain.ExerciseTrackingType
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

/**
 * Write path for the WorkoutTemplate aggregate (workout_templates +
 * template_exercises). Same delete-then-reinsert aggregate replacement
 * as WorkoutSessionSyncRepository - see its header for the rationale.
 */
@Repository
class WorkoutTemplateSyncRepository(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    fun find(id: String): StoredEntity<WorkoutTemplateSyncDto>? {
        val sql = """
            SELECT id, title, source, owner_id, derived_from_id, deprecated, updated_at, revision, deleted_at
            FROM workout_templates WHERE id = :id::uuid
        """.trimIndent()
        val stored = jdbcTemplate.query(sql, MapSqlParameterSource("id", id)) { rs, _ -> mapTemplateRow(rs) }
            .firstOrNull() ?: return null
        return stored.copy(dto = stored.dto.copy(templateExercises = findTemplateExercises(id)))
    }

    /** deletedAt is the resolved value to persist: the caller has already turned "client asked to delete" into the server's own timestamp, or null. */
    fun insert(uid: String, dto: WorkoutTemplateSyncDto, now: Long, deletedAt: Long?) {
        val sql = """
            INSERT INTO workout_templates (id, title, source, owner_id, derived_from_id, deprecated, updated_at, revision, deleted_at)
            VALUES (:id::uuid, :title, :source, :ownerId, :derivedFromId::uuid, :deprecated, :now, 1, :deletedAt)
        """.trimIndent()
        jdbcTemplate.update(sql, templateParams(dto).addValue("ownerId", uid).addValue("now", now).addValue("deletedAt", deletedAt))
        replaceChildren(dto.id, dto.templateExercises)
    }

    fun update(dto: WorkoutTemplateSyncDto, newRevision: Int, now: Long, deletedAt: Long?) {
        val sql = """
            UPDATE workout_templates
            SET title = :title, source = :source, derived_from_id = :derivedFromId::uuid, deprecated = :deprecated,
                updated_at = :now, revision = :newRevision, deleted_at = :deletedAt
            WHERE id = :id::uuid
        """.trimIndent()
        jdbcTemplate.update(
            sql,
            templateParams(dto).addValue("now", now).addValue("newRevision", newRevision).addValue("deletedAt", deletedAt),
        )
        replaceChildren(dto.id, dto.templateExercises)
    }

    private fun templateParams(dto: WorkoutTemplateSyncDto): MapSqlParameterSource =
        MapSqlParameterSource()
            .addValue("id", dto.id)
            .addValue("title", dto.title)
            .addValue("source", dto.source.name)
            .addValue("derivedFromId", dto.derivedFromId)
            .addValue("deprecated", dto.deprecated)

    private fun replaceChildren(templateId: String, exercises: List<TemplateExerciseSyncDto>) {
        jdbcTemplate.update(
            "DELETE FROM template_exercises WHERE template_id = :templateId::uuid",
            MapSqlParameterSource("templateId", templateId),
        )
        if (exercises.isEmpty()) return

        val sql = """
            INSERT INTO template_exercises
                (id, template_id, exercise_id, exercise_name, exercise_tracking_type, planned_sets, planned_reps,
                 order_index, rest_seconds, planned_duration_seconds, planned_distance_meters, planned_weight_kg, notes)
            VALUES
                (:id::uuid, :templateId::uuid, :exerciseId::uuid, :exerciseName, :exerciseTrackingType, :plannedSets, :plannedReps,
                 :orderIndex, :restSeconds, :plannedDurationSeconds, :plannedDistanceMeters, :plannedWeightKg, :notes)
        """.trimIndent()
        val params = exercises.map { te ->
            MapSqlParameterSource()
                .addValue("id", te.id)
                .addValue("templateId", templateId)
                .addValue("exerciseId", te.exerciseId)
                .addValue("exerciseName", te.exerciseName)
                .addValue("exerciseTrackingType", te.exerciseTrackingType.name)
                .addValue("plannedSets", te.plannedSets)
                .addValue("plannedReps", te.plannedReps)
                .addValue("orderIndex", te.orderIndex)
                .addValue("restSeconds", te.restSeconds)
                .addValue("plannedDurationSeconds", te.plannedDurationSeconds)
                .addValue("plannedDistanceMeters", te.plannedDistanceMeters)
                .addValue("plannedWeightKg", te.plannedWeightKg)
                .addValue("notes", te.notes)
        }.toTypedArray()
        jdbcTemplate.batchUpdate(sql, params)
    }

    private fun findTemplateExercises(templateId: String): List<TemplateExerciseSyncDto> {
        val sql = """
            SELECT id, exercise_id, exercise_name, exercise_tracking_type, planned_sets, planned_reps, order_index,
                   rest_seconds, planned_duration_seconds, planned_distance_meters, planned_weight_kg, notes
            FROM template_exercises WHERE template_id = :templateId::uuid ORDER BY order_index
        """.trimIndent()
        return jdbcTemplate.query(sql, MapSqlParameterSource("templateId", templateId)) { rs, _ -> mapTemplateExerciseRow(rs) }
    }

    private fun mapTemplateRow(rs: ResultSet): StoredEntity<WorkoutTemplateSyncDto> {
        val dto = WorkoutTemplateSyncDto(
            id = rs.getString("id"),
            title = rs.getString("title"),
            templateExercises = emptyList(),
            source = EntitySource.valueOf(rs.getString("source")),
            derivedFromId = rs.getString("derived_from_id"),
            deprecated = rs.getBoolean("deprecated"),
            revision = rs.getInt("revision"),
            deletedAt = rs.getNullableLong("deleted_at"),
        )
        val ownerId = rs.getString("owner_id") ?: ""
        return StoredEntity(ownerId, rs.getInt("revision"), rs.getLong("updated_at"), dto)
    }

    private fun mapTemplateExerciseRow(rs: ResultSet): TemplateExerciseSyncDto = TemplateExerciseSyncDto(
        id = rs.getString("id"),
        exerciseId = rs.getString("exercise_id"),
        exerciseName = rs.getString("exercise_name"),
        exerciseTrackingType = ExerciseTrackingType.valueOf(rs.getString("exercise_tracking_type")),
        plannedSets = rs.getInt("planned_sets"),
        plannedReps = rs.getNullableInt("planned_reps"),
        orderIndex = rs.getInt("order_index"),
        restSeconds = rs.getNullableInt("rest_seconds"),
        plannedDurationSeconds = rs.getNullableInt("planned_duration_seconds"),
        plannedDistanceMeters = rs.getNullableFloat("planned_distance_meters"),
        plannedWeightKg = rs.getNullableFloat("planned_weight_kg"),
        notes = rs.getString("notes"),
    )
}
