package com.rukavina.gymbuddy.data.repository

import androidx.room.withTransaction
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.local.entity.OutboxOperation
import com.rukavina.gymbuddy.data.local.entity.StarterPlanEntity
import com.rukavina.gymbuddy.data.local.entity.StarterPlanTemplateEntity
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.domain.model.StarterPlan
import com.rukavina.gymbuddy.domain.model.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import javax.inject.Inject

/**
 * Starter plans (reference data, read-only) and the user's chosen plan.
 * The choice is UserProfile.activePlanId (owner decision 2A), so it
 * syncs with the profile.
 */
class StarterPlanRepository @Inject constructor(
    private val holder: DatabaseHolder,
    private val clock: Clock
) {

    /** All plans in server order, deprecated ones included (an activePlanId may point at one). */
    fun observePlans(): Flow<List<StarterPlan>> = holder.observe { db ->
        val dao = db.starterPlanDao()
        combine(dao.observePlans(), dao.observePlanTemplates()) { plans, rows -> toDomain(plans, rows) }
    }

    suspend fun getPlans(): List<StarterPlan> {
        val dao = holder.db.starterPlanDao()
        return toDomain(dao.getPlans(), dao.getPlanTemplates())
    }

    /**
     * Sets (or clears, with null) the open owner's active plan. Only
     * that field changes; the rest of the stored profile is kept.
     * Returns false when the open file has no profile for the owner
     * (also when the owner switched mid-call: files are per owner).
     */
    suspend fun setActivePlan(planId: String?): Boolean {
        val uid = holder.owner ?: UserProfileRepository.GUEST_UID
        val db = holder.db
        return db.withTransaction {
            val profiles = db.userProfileDao()
            val profile = profiles.getUserProfile(uid) ?: return@withTransaction false
            if (profile.activePlanId == planId) return@withTransaction true
            val now = clock.millis()
            profiles.insertUserProfile(profile.copy(activePlanId = planId, updatedAt = now, syncState = SyncState.PENDING))
            db.syncDao().enqueue(SyncEntityType.USER_PROFILE, uid, OutboxOperation.UPSERT, now)
            true
        }
    }

    private fun toDomain(plans: List<StarterPlanEntity>, rows: List<StarterPlanTemplateEntity>): List<StarterPlan> {
        val templateIds = rows.groupBy({ it.planId }, { it }).mapValues { (_, r) -> r.sortedBy { it.orderIndex }.map { it.templateId } }
        return plans.map {
            StarterPlan(
                id = it.id,
                title = it.title,
                description = it.description,
                templateIds = templateIds[it.id].orEmpty(),
                suggestedFor = it.suggestedFor,
                deprecated = it.deprecated
            )
        }
    }
}
