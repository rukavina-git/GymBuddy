package com.rukavina.gymbuddy.data.repository

import com.rukavina.gymbuddy.data.local.dao.WorkoutTemplateDao
import com.rukavina.gymbuddy.data.local.mapper.WorkoutTemplateMapper
import com.rukavina.gymbuddy.data.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of WorkoutTemplateRepository.
 * Currently uses only local Room database.
 * Can be extended to sync with remote API in the future.
 *
 * Follows the same pattern as WorkoutSessionRepositoryImpl for consistency.
 */
class WorkoutTemplateRepositoryImpl @Inject constructor(
    private val workoutTemplateDao: WorkoutTemplateDao
) : WorkoutTemplateRepository {

    override fun getAllTemplates(): Flow<List<WorkoutTemplate>> {
        return workoutTemplateDao.getAllTemplates().map { templatesWithExercises ->
            WorkoutTemplateMapper.toDomainList(templatesWithExercises)
        }
    }

    override suspend fun getTemplateById(id: String): WorkoutTemplate? {
        val templateWithExercises = workoutTemplateDao.getTemplateById(id)
        return templateWithExercises?.let { WorkoutTemplateMapper.toDomain(it) }
    }

    override fun searchTemplates(query: String): Flow<List<WorkoutTemplate>> {
        return workoutTemplateDao.searchTemplates(query).map { templatesWithExercises ->
            WorkoutTemplateMapper.toDomainList(templatesWithExercises)
        }
    }

    override suspend fun createTemplate(template: WorkoutTemplate) {
        val (templateEntity, exerciseEntities) = WorkoutTemplateMapper.toEntities(template)
        workoutTemplateDao.insertTemplateWithExercises(templateEntity, exerciseEntities)
        // TODO: Sync with remote API when online
    }

    override suspend fun updateTemplate(template: WorkoutTemplate) {
        val (templateEntity, exerciseEntities) = WorkoutTemplateMapper.toEntities(template)
        workoutTemplateDao.updateTemplateWithExercises(templateEntity, exerciseEntities)
        // TODO: Sync with remote API when online
    }

    override suspend fun deleteTemplate(id: String) {
        workoutTemplateDao.deleteTemplate(id)
        // TODO: Sync deletion with remote API when online
    }
}
