package com.rukavina.gymbuddy.persistence

import com.rukavina.gymbuddy.api.dto.BodyWeightEntryDto
import com.rukavina.gymbuddy.api.dto.BodyWeightEntrySyncDto
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

/** Read and write paths for body_weight_entries (V5). Flat entity, no children. */
@Repository
class BodyWeightEntryRepository(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    fun find(id: String): StoredEntity<BodyWeightEntrySyncDto>? {
        val sql = """
            SELECT id, owner_id, measured_at, weight_kg, updated_at, deleted_at, revision
            FROM body_weight_entries WHERE id = :id::uuid
        """.trimIndent()
        return jdbcTemplate.query(sql, MapSqlParameterSource("id", id)) { rs, _ ->
            StoredEntity(
                ownerId = rs.getString("owner_id"),
                revision = rs.getInt("revision"),
                updatedAt = rs.getLong("updated_at"),
                dto = BodyWeightEntrySyncDto(
                    id = rs.getString("id"),
                    measuredAt = rs.getLong("measured_at"),
                    weightKg = rs.getFloat("weight_kg"),
                    revision = rs.getInt("revision"),
                    deletedAt = rs.getNullableLong("deleted_at"),
                ),
            )
        }.firstOrNull()
    }

    /** deletedAt is the resolved value to persist (the server's own time, or null). */
    fun insert(uid: String, dto: BodyWeightEntrySyncDto, now: Long, deletedAt: Long?) {
        val sql = """
            INSERT INTO body_weight_entries (id, owner_id, measured_at, weight_kg, updated_at, deleted_at, revision)
            VALUES (:id::uuid, :ownerId, :measuredAt, :weightKg, :now, :deletedAt, 1)
        """.trimIndent()
        jdbcTemplate.update(sql, params(dto).addValue("ownerId", uid).addValue("now", now).addValue("deletedAt", deletedAt))
    }

    fun update(dto: BodyWeightEntrySyncDto, newRevision: Int, now: Long, deletedAt: Long?) {
        val sql = """
            UPDATE body_weight_entries
            SET measured_at = :measuredAt, weight_kg = :weightKg, updated_at = :now,
                revision = :newRevision, deleted_at = :deletedAt
            WHERE id = :id::uuid
        """.trimIndent()
        jdbcTemplate.update(
            sql,
            params(dto).addValue("now", now).addValue("newRevision", newRevision).addValue("deletedAt", deletedAt),
        )
    }

    fun findAllByIds(userId: String, ids: Collection<String>): List<BodyWeightEntryDto> {
        if (ids.isEmpty()) return emptyList()
        val sql = """
            SELECT id, measured_at, weight_kg, updated_at, deleted_at, revision
            FROM body_weight_entries
            WHERE owner_id = :userId AND id::text IN (:ids)
        """.trimIndent()
        return jdbcTemplate.query(sql, mapOf("userId" to userId, "ids" to ids)) { rs, _ ->
            BodyWeightEntryDto(
                id = rs.getString("id"),
                measuredAt = rs.getLong("measured_at"),
                weightKg = rs.getFloat("weight_kg"),
                updatedAt = rs.getLong("updated_at"),
                deletedAt = rs.getNullableLong("deleted_at"),
                revision = rs.getInt("revision"),
            )
        }
    }

    private fun params(dto: BodyWeightEntrySyncDto): MapSqlParameterSource =
        MapSqlParameterSource()
            .addValue("id", dto.id)
            .addValue("measuredAt", dto.measuredAt)
            .addValue("weightKg", dto.weightKg)
}
