package com.rukavina.gymbuddy.persistence

import com.rukavina.gymbuddy.api.dto.TemplateExerciseDto
import com.rukavina.gymbuddy.api.dto.WorkoutTemplateDto
import com.rukavina.gymbuddy.domain.EntitySource
import com.rukavina.gymbuddy.domain.ExerciseTrackingType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

/**
 * The default (server-owned) workout template library only - GET
 * /v1/reference/templates per api/openapi.yaml. Same rationale as
 * ExerciseReferenceRepository: deprecated rows included, no owner
 * parameter since this isn't user data.
 */
@Repository
class WorkoutTemplateReferenceRepository(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    fun findAllDefault(): List<WorkoutTemplateDto> {
        val templatesSql = """
            SELECT id, title, source, owner_id, derived_from_id, deprecated, updated_at, deleted_at, revision
            FROM workout_templates
            WHERE source = 'DEFAULT'
            ORDER BY title
        """.trimIndent()
        val templates = jdbcTemplate.query(templatesSql, emptyMap<String, Any>()) { rs, _ -> mapTemplateRow(rs) }

        val exercisesSql = """
            SELECT te.id, te.template_id, te.exercise_id, te.exercise_name, te.exercise_tracking_type,
                   te.planned_sets, te.planned_reps, te.order_index, te.rest_seconds,
                   te.planned_duration_seconds, te.planned_distance_meters, te.planned_weight_kg, te.notes
            FROM template_exercises te
            JOIN workout_templates wt ON wt.id = te.template_id
            WHERE wt.source = 'DEFAULT'
            ORDER BY te.template_id, te.order_index
        """.trimIndent()
        val exercisesByTemplateId = jdbcTemplate.query(exercisesSql, emptyMap<String, Any>()) { rs, _ ->
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

internal fun ResultSet.getNullableInt(column: String): Int? =
    getInt(column).takeUnless { wasNull() }
