package com.rukavina.gymbuddy.persistence

import com.rukavina.gymbuddy.api.dto.TemplateExerciseDto
import com.rukavina.gymbuddy.api.dto.WorkoutTemplateDto
import com.rukavina.gymbuddy.domain.EntitySource
import com.rukavina.gymbuddy.domain.ExerciseTrackingType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

/**
 * Read path for pull: bulk-loads custom WorkoutTemplate aggregates by
 * id, reusing WorkoutTemplateDto/TemplateExerciseDto (ReferenceDtos.kt)
 * directly - same shape the reference-library endpoint already uses,
 * just scoped to one owner's ids instead of source = 'DEFAULT'.
 */
@Repository
class WorkoutTemplatePullRepository(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    fun findAllByIds(userId: String, ids: Collection<String>): List<WorkoutTemplateDto> {
        if (ids.isEmpty()) return emptyList()

        val templatesSql = """
            SELECT id, title, source, owner_id, derived_from_id, deprecated, updated_at, deleted_at, revision
            FROM workout_templates
            WHERE owner_id = :userId AND id::text IN (:ids)
        """.trimIndent()
        val templates = jdbcTemplate.query(templatesSql, mapOf("userId" to userId, "ids" to ids)) { rs, _ -> mapTemplateRow(rs) }
        if (templates.isEmpty()) return emptyList()

        val templateIds = templates.map { it.id }
        val exercisesSql = """
            SELECT id, template_id, exercise_id, exercise_name, exercise_tracking_type, planned_sets, planned_reps,
                   order_index, rest_seconds, planned_duration_seconds, planned_distance_meters, planned_weight_kg, notes
            FROM template_exercises
            WHERE template_id::text IN (:templateIds)
            ORDER BY template_id, order_index
        """.trimIndent()
        val exercisesByTemplateId = jdbcTemplate.query(exercisesSql, mapOf("templateIds" to templateIds)) { rs, _ ->
            rs.getString("template_id") to mapTemplateExerciseRow(rs)
        }.groupBy(keySelector = { it.first }, valueTransform = { it.second })

        return templates.map { it.copy(templateExercises = exercisesByTemplateId[it.id].orEmpty()) }
    }

    private fun mapTemplateRow(rs: ResultSet): WorkoutTemplateDto = WorkoutTemplateDto(
        id = rs.getString("id"),
        title = rs.getString("title"),
        templateExercises = emptyList(),
        source = EntitySource.valueOf(rs.getString("source")),
        ownerId = rs.getString("owner_id"),
        derivedFromId = rs.getString("derived_from_id"),
        deprecated = rs.getBoolean("deprecated"),
        updatedAt = rs.getLong("updated_at"),
        deletedAt = rs.getNullableLong("deleted_at"),
        revision = rs.getInt("revision"),
    )

    private fun mapTemplateExerciseRow(rs: ResultSet): TemplateExerciseDto = TemplateExerciseDto(
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
