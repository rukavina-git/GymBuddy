package com.rukavina.gymbuddy.persistence

import com.rukavina.gymbuddy.api.dto.StarterPlanDto
import com.rukavina.gymbuddy.domain.TrainingDays
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

/**
 * Starter plans for GET /v1/reference/templates. Reference data like
 * the default templates: deprecated plans included, no owner.
 */
@Repository
class StarterPlanReferenceRepository(private val jdbcTemplate: NamedParameterJdbcTemplate) {

    fun findAll(): List<StarterPlanDto> {
        val templateIdsByPlan = jdbcTemplate.query(
            "SELECT plan_id, template_id FROM starter_plan_templates ORDER BY plan_id, order_index",
            emptyMap<String, Any>(),
        ) { rs, _ -> rs.getString("plan_id") to rs.getString("template_id") }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })

        return jdbcTemplate.query(
            "SELECT id, title, description, suggested_for, deprecated FROM starter_plans ORDER BY display_order",
            emptyMap<String, Any>(),
        ) { rs, _ ->
            val id = rs.getString("id")
            StarterPlanDto(
                id = id,
                title = rs.getString("title"),
                description = rs.getString("description"),
                templateIds = templateIdsByPlan[id].orEmpty(),
                suggestedFor = rs.getString("suggested_for")?.let { TrainingDays.valueOf(it) },
                deprecated = rs.getBoolean("deprecated"),
            )
        }
    }
}
