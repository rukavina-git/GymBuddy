package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rukavina.gymbuddy.domain.model.TrainingDays

/**
 * A starter plan (Plan 6 step 06): reference data, replaced as a whole
 * with the default template library. Never written by the user.
 */
@Entity(tableName = "starter_plans")
data class StarterPlanEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val suggestedFor: TrainingDays?,
    /** Position in the server's list. */
    val displayOrder: Int,
    val deprecated: Boolean
)

/**
 * One template in a plan's rotation. templateId is a soft reference
 * (no foreign key), like every cross-aggregate reference.
 */
@Entity(
    tableName = "starter_plan_templates",
    primaryKeys = ["planId", "orderIndex"],
    foreignKeys = [
        ForeignKey(
            entity = StarterPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("templateId")]
)
data class StarterPlanTemplateEntity(
    val planId: String,
    val orderIndex: Int,
    val templateId: String
)
