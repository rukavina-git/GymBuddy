package com.rukavina.gymbuddy.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.rukavina.gymbuddy.api.dto.PerformedExerciseSyncDto
import com.rukavina.gymbuddy.api.dto.WorkoutSessionSyncDto
import com.rukavina.gymbuddy.api.dto.WorkoutSetSyncDto
import com.rukavina.gymbuddy.domain.ExerciseCategory
import com.rukavina.gymbuddy.domain.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.MuscleGroup
import com.rukavina.gymbuddy.domain.SetType
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

/** A stored row's ownership/version bookkeeping alongside its mapped content. */
data class StoredEntity<T>(val ownerId: String, val revision: Int, val updatedAt: Long, val dto: T)

/**
 * Write path for the WorkoutSession aggregate (workout_sessions +
 * performed_exercises + workout_sets). Aggregate replacement per Group
 * F point 5: update/insert always deletes every existing child and
 * reinserts the pushed tree, in the same transaction as the parent -
 * never a child-level merge.
 */
@Repository
class WorkoutSessionSyncRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper,
) {

    fun find(id: String): StoredEntity<WorkoutSessionSyncDto>? {
        val sql = """
            SELECT id, owner_id, started_at, ended_at, duration_seconds, title, notes, template_id,
                   template_title, updated_at, revision, deleted_at
            FROM workout_sessions WHERE id = :id::uuid
        """.trimIndent()
        val stored = jdbcTemplate.query(sql, MapSqlParameterSource("id", id)) { rs, _ -> mapSessionRow(rs) }
            .firstOrNull() ?: return null
        return stored.copy(dto = stored.dto.copy(performedExercises = findPerformedExercises(id)))
    }

    /** deletedAt is the resolved value to persist: the caller has already turned "client asked to delete" into the server's own timestamp, or null. */
    fun insert(uid: String, dto: WorkoutSessionSyncDto, now: Long, deletedAt: Long?) {
        val sql = """
            INSERT INTO workout_sessions
                (id, owner_id, started_at, ended_at, duration_seconds, title, notes, template_id,
                 template_title, updated_at, revision, deleted_at)
            VALUES (:id::uuid, :ownerId, :startedAt, :endedAt, :durationSeconds, :title, :notes, :templateId::uuid,
                    :templateTitle, :now, 1, :deletedAt)
        """.trimIndent()
        jdbcTemplate.update(sql, sessionParams(dto).addValue("ownerId", uid).addValue("now", now).addValue("deletedAt", deletedAt))
        replaceChildren(dto.id, dto.performedExercises)
    }

    fun update(dto: WorkoutSessionSyncDto, newRevision: Int, now: Long, deletedAt: Long?) {
        val sql = """
            UPDATE workout_sessions
            SET started_at = :startedAt, ended_at = :endedAt, duration_seconds = :durationSeconds,
                title = :title, notes = :notes, template_id = :templateId::uuid, template_title = :templateTitle,
                updated_at = :now, revision = :newRevision, deleted_at = :deletedAt
            WHERE id = :id::uuid
        """.trimIndent()
        jdbcTemplate.update(
            sql,
            sessionParams(dto).addValue("now", now).addValue("newRevision", newRevision).addValue("deletedAt", deletedAt),
        )
        replaceChildren(dto.id, dto.performedExercises)
    }

    private fun sessionParams(dto: WorkoutSessionSyncDto): MapSqlParameterSource =
        MapSqlParameterSource()
            .addValue("id", dto.id)
            .addValue("startedAt", dto.startedAt)
            .addValue("endedAt", dto.endedAt)
            .addValue("durationSeconds", dto.durationSeconds)
            .addValue("title", dto.title)
            .addValue("notes", dto.notes)
            .addValue("templateId", dto.templateId)
            .addValue("templateTitle", dto.templateTitle)

    private fun replaceChildren(sessionId: String, exercises: List<PerformedExerciseSyncDto>) {
        jdbcTemplate.update(
            "DELETE FROM performed_exercises WHERE workout_session_id = :sessionId::uuid",
            MapSqlParameterSource("sessionId", sessionId),
        )
        if (exercises.isEmpty()) return

        val peSql = """
            INSERT INTO performed_exercises
                (id, workout_session_id, exercise_id, order_index, exercise_name, exercise_category,
                 exercise_tracking_type, exercise_primary_muscles, superset_group)
            VALUES (:id::uuid, :sessionId::uuid, :exerciseId::uuid, :orderIndex, :exerciseName, :exerciseCategory,
                    :exerciseTrackingType, :exercisePrimaryMuscles::jsonb, :supersetGroup)
        """.trimIndent()
        val peParams = exercises.map { pe ->
            MapSqlParameterSource()
                .addValue("id", pe.id)
                .addValue("sessionId", sessionId)
                .addValue("exerciseId", pe.exerciseId)
                .addValue("orderIndex", pe.orderIndex)
                .addValue("exerciseName", pe.exerciseName)
                .addValue("exerciseCategory", pe.exerciseCategory.name)
                .addValue("exerciseTrackingType", pe.exerciseTrackingType.name)
                .addValue("exercisePrimaryMuscles", objectMapper.writeValueAsString(pe.exercisePrimaryMuscles.map { it.name }))
                .addValue("supersetGroup", pe.supersetGroup)
        }.toTypedArray()
        jdbcTemplate.batchUpdate(peSql, peParams)

        val setSql = """
            INSERT INTO workout_sets
                (id, performed_exercise_id, weight_kg, reps, duration_seconds, distance_meters, set_type,
                 is_completed, rest_taken_seconds, order_index)
            VALUES (:id::uuid, :performedExerciseId::uuid, :weightKg, :reps, :durationSeconds, :distanceMeters, :setType,
                    :isCompleted, :restTakenSeconds, :orderIndex)
        """.trimIndent()
        val setParams = exercises.flatMap { pe -> pe.sets.map { set -> pe.id to set } }
            .map { (performedExerciseId, set) ->
                MapSqlParameterSource()
                    .addValue("id", set.id)
                    .addValue("performedExerciseId", performedExerciseId)
                    .addValue("weightKg", set.weightKg)
                    .addValue("reps", set.reps)
                    .addValue("durationSeconds", set.durationSeconds)
                    .addValue("distanceMeters", set.distanceMeters)
                    .addValue("setType", set.setType.name)
                    .addValue("isCompleted", set.isCompleted)
                    .addValue("restTakenSeconds", set.restTakenSeconds)
                    .addValue("orderIndex", set.orderIndex)
            }.toTypedArray()
        if (setParams.isNotEmpty()) jdbcTemplate.batchUpdate(setSql, setParams)
    }

    private fun findPerformedExercises(sessionId: String): List<PerformedExerciseSyncDto> {
        val peSql = """
            SELECT id, exercise_id, order_index, exercise_name, exercise_category, exercise_tracking_type,
                   exercise_primary_muscles, superset_group
            FROM performed_exercises WHERE workout_session_id = :sessionId::uuid ORDER BY order_index
        """.trimIndent()
        val exercises = jdbcTemplate.query(peSql, MapSqlParameterSource("sessionId", sessionId)) { rs, _ -> mapPerformedExerciseRow(rs) }

        val setsSql = """
            SELECT ws.id, ws.performed_exercise_id, ws.weight_kg, ws.reps, ws.duration_seconds, ws.distance_meters,
                   ws.set_type, ws.is_completed, ws.rest_taken_seconds, ws.order_index
            FROM workout_sets ws
            JOIN performed_exercises pe ON pe.id = ws.performed_exercise_id
            WHERE pe.workout_session_id = :sessionId::uuid
            ORDER BY ws.performed_exercise_id, ws.order_index
        """.trimIndent()
        val setsByPerformedExerciseId = jdbcTemplate.query(setsSql, MapSqlParameterSource("sessionId", sessionId)) { rs, _ ->
            rs.getString("performed_exercise_id") to mapSetRow(rs)
        }.groupBy(keySelector = { it.first }, valueTransform = { it.second })

        return exercises.map { it.copy(sets = setsByPerformedExerciseId[it.id].orEmpty()) }
    }

    private fun mapSessionRow(rs: ResultSet): StoredEntity<WorkoutSessionSyncDto> {
        val dto = WorkoutSessionSyncDto(
            id = rs.getString("id"),
            startedAt = rs.getLong("started_at"),
            endedAt = rs.getNullableLong("ended_at"),
            durationSeconds = rs.getInt("duration_seconds"),
            title = rs.getString("title"),
            notes = rs.getString("notes"),
            templateId = rs.getString("template_id"),
            templateTitle = rs.getString("template_title"),
            performedExercises = emptyList(),
            revision = rs.getInt("revision"),
            deletedAt = rs.getNullableLong("deleted_at"),
        )
        return StoredEntity(rs.getString("owner_id"), rs.getInt("revision"), rs.getLong("updated_at"), dto)
    }

    private fun mapPerformedExerciseRow(rs: ResultSet): PerformedExerciseSyncDto = PerformedExerciseSyncDto(
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

    private fun mapSetRow(rs: ResultSet): WorkoutSetSyncDto = WorkoutSetSyncDto(
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
