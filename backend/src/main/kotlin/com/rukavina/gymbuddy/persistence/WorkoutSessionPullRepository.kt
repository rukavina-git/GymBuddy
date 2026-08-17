package com.rukavina.gymbuddy.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.rukavina.gymbuddy.api.dto.PerformedExerciseDto
import com.rukavina.gymbuddy.api.dto.WorkoutSessionDto
import com.rukavina.gymbuddy.api.dto.WorkoutSetDto
import com.rukavina.gymbuddy.domain.ExerciseCategory
import com.rukavina.gymbuddy.domain.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.MuscleGroup
import com.rukavina.gymbuddy.domain.SetType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

/**
 * Read path for pull: bulk-loads the current state of WorkoutSession
 * aggregates by id, response-shaped (WorkoutSessionDto, with
 * updatedAt/deletedAt/revision always present) rather than the
 * push-shaped WorkoutSessionSyncRepository/WorkoutSessionSyncDto -
 * that repository's insert/update/find aren't reused here since they
 * speak a different DTO family entirely.
 */
@Repository
class WorkoutSessionPullRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper,
) {

    /** ids are trusted to already be scoped to this user (see ChangeLogReader/FullSyncEntityDiscovery); owner_id is still filtered here too, in depth. */
    fun findAllByIds(userId: String, ids: Collection<String>): List<WorkoutSessionDto> {
        if (ids.isEmpty()) return emptyList()

        val sql = """
            SELECT id, started_at, ended_at, duration_seconds, title, notes, template_id, template_title,
                   updated_at, deleted_at, revision
            FROM workout_sessions
            WHERE owner_id = :userId AND id::text IN (:ids)
        """.trimIndent()
        val sessions = jdbcTemplate.query(sql, mapOf("userId" to userId, "ids" to ids)) { rs, _ -> mapSessionRow(rs) }
        if (sessions.isEmpty()) return emptyList()

        val sessionIds = sessions.map { it.id }
        val peSql = """
            SELECT id, workout_session_id, exercise_id, order_index, exercise_name, exercise_category,
                   exercise_tracking_type, exercise_primary_muscles, superset_group
            FROM performed_exercises
            WHERE workout_session_id::text IN (:sessionIds)
            ORDER BY workout_session_id, order_index
        """.trimIndent()
        val performedExercisesBySessionId = jdbcTemplate.query(peSql, mapOf("sessionIds" to sessionIds)) { rs, _ ->
            rs.getString("workout_session_id") to mapPerformedExerciseRow(rs)
        }

        val performedExerciseIds = performedExercisesBySessionId.map { it.second.id }
        val setsByPerformedExerciseId = if (performedExerciseIds.isEmpty()) {
            emptyMap()
        } else {
            val setsSql = """
                SELECT id, performed_exercise_id, weight_kg, reps, duration_seconds, distance_meters, set_type,
                       is_completed, rest_taken_seconds, order_index
                FROM workout_sets
                WHERE performed_exercise_id::text IN (:performedExerciseIds)
                ORDER BY performed_exercise_id, order_index
            """.trimIndent()
            jdbcTemplate.query(setsSql, mapOf("performedExerciseIds" to performedExerciseIds)) { rs, _ ->
                rs.getString("performed_exercise_id") to mapSetRow(rs)
            }.groupBy(keySelector = { it.first }, valueTransform = { it.second })
        }

        val groupedPerformedExercises = performedExercisesBySessionId
            .map { (sessionId, pe) -> sessionId to pe.copy(sets = setsByPerformedExerciseId[pe.id].orEmpty()) }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })

        return sessions.map { it.copy(performedExercises = groupedPerformedExercises[it.id].orEmpty()) }
    }

    private fun mapSessionRow(rs: ResultSet): WorkoutSessionDto = WorkoutSessionDto(
        id = rs.getString("id"),
        startedAt = rs.getLong("started_at"),
        endedAt = rs.getNullableLong("ended_at"),
        durationSeconds = rs.getInt("duration_seconds"),
        title = rs.getString("title"),
        notes = rs.getString("notes"),
        templateId = rs.getString("template_id"),
        templateTitle = rs.getString("template_title"),
        performedExercises = emptyList(),
        updatedAt = rs.getLong("updated_at"),
        deletedAt = rs.getNullableLong("deleted_at"),
        revision = rs.getInt("revision"),
    )

    private fun mapPerformedExerciseRow(rs: ResultSet): PerformedExerciseDto = PerformedExerciseDto(
        id = rs.getString("id"),
        exerciseId = rs.getString("exercise_id"),
        orderIndex = rs.getInt("order_index"),
        exerciseName = rs.getString("exercise_name"),
        exerciseCategory = ExerciseCategory.valueOf(rs.getString("exercise_category")),
        exerciseTrackingType = ExerciseTrackingType.valueOf(rs.getString("exercise_tracking_type")),
        exercisePrimaryMuscles = objectMapper.readStringArrayColumn(rs, "exercise_primary_muscles").map { MuscleGroup.valueOf(it) },
        sets = emptyList(),
        supersetGroup = rs.getNullableInt("superset_group"),
    )

    private fun mapSetRow(rs: ResultSet): WorkoutSetDto = WorkoutSetDto(
        id = rs.getString("id"),
        weightKg = rs.getNullableFloat("weight_kg"),
        reps = rs.getNullableInt("reps"),
        durationSeconds = rs.getNullableInt("duration_seconds"),
        distanceMeters = rs.getNullableFloat("distance_meters"),
        setType = SetType.valueOf(rs.getString("set_type")),
        isCompleted = rs.getBoolean("is_completed"),
        restTakenSeconds = rs.getNullableInt("rest_taken_seconds"),
        orderIndex = rs.getInt("order_index"),
    )
}
