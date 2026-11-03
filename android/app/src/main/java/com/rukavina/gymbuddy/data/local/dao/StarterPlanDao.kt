package com.rukavina.gymbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rukavina.gymbuddy.data.local.entity.StarterPlanEntity
import com.rukavina.gymbuddy.data.local.entity.StarterPlanTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StarterPlanDao {

    @Query("SELECT * FROM starter_plans ORDER BY displayOrder")
    fun observePlans(): Flow<List<StarterPlanEntity>>

    @Query("SELECT * FROM starter_plan_templates ORDER BY planId, orderIndex")
    fun observePlanTemplates(): Flow<List<StarterPlanTemplateEntity>>

    @Query("SELECT * FROM starter_plans ORDER BY displayOrder")
    suspend fun getPlans(): List<StarterPlanEntity>

    @Query("SELECT * FROM starter_plan_templates ORDER BY planId, orderIndex")
    suspend fun getPlanTemplates(): List<StarterPlanTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<StarterPlanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanTemplates(rows: List<StarterPlanTemplateEntity>)

    /** Plan templates go with it (CASCADE). */
    @Query("DELETE FROM starter_plans")
    suspend fun deleteAll()
}
