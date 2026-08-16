package com.rukavina.gymbuddy.persistence

import com.rukavina.gymbuddy.api.dto.ReferenceVersionDto
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

/**
 * reference_version is a single server-owned row (id = 1), not
 * user-owned data, so unlike the other repositories in this package it
 * takes no owner parameter - there is nothing to isolate by owner.
 */
@Repository
class ReferenceVersionRepository(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    fun get(): ReferenceVersionDto {
        val sql = "SELECT exercise_library_version, template_library_version FROM reference_version WHERE id = 1"
        return jdbcTemplate.queryForObject(sql, emptyMap<String, Any>()) { rs, _ ->
            ReferenceVersionDto(
                exerciseLibraryVersion = rs.getInt("exercise_library_version"),
                templateLibraryVersion = rs.getInt("template_library_version"),
            )
        } ?: error("reference_version row (id = 1) is missing - Flyway migration V2 should have seeded it")
    }
}
