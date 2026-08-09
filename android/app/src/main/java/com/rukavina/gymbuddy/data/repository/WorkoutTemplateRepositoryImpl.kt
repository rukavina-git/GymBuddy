package com.rukavina.gymbuddy.data.repository

import com.rukavina.gymbuddy.data.local.dao.WorkoutTemplateDao
import com.rukavina.gymbuddy.data.local.mapper.WorkoutTemplateMapper
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
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
        requireStampedSnapshots(template)
        val (templateEntity, exerciseEntities) = WorkoutTemplateMapper.toEntities(template)
        workoutTemplateDao.insertTemplateWithExercises(templateEntity, exerciseEntities)
        // TODO: Sync with remote API when online
    }

    override suspend fun updateTemplate(template: WorkoutTemplate) {
        requireStampedSnapshots(template)
        val (templateEntity, exerciseEntities) = WorkoutTemplateMapper.toEntities(template)
        workoutTemplateDao.updateTemplateWithExercises(templateEntity, exerciseEntities)
        // TODO: Sync with remote API when online
    }

    override suspend fun deleteTemplate(id: String) {
        workoutTemplateDao.deleteTemplate(id)
        // TODO: Sync deletion with remote API when online
    }

    override suspend fun hideTemplate(id: String) {
        workoutTemplateDao.hideTemplate(id)
    }

    override suspend fun unhideTemplate(id: String) {
        workoutTemplateDao.unhideTemplate(id)
    }

    override fun getHiddenTemplates(): Flow<List<WorkoutTemplate>> {
        return workoutTemplateDao.getHiddenTemplates().map { templatesWithExercises ->
            WorkoutTemplateMapper.toDomainList(templatesWithExercises)
        }
    }

    /**
     * Guards against ever persisting a TemplateExercise whose exercise
     * snapshot was never stamped. Construction sites are allowed to build a
     * TemplateExercise with a placeholder exerciseName, relying on
     * StampTemplateExerciseSnapshotsUseCase to overwrite it before the
     * template reaches a write method - this is the boundary every write
     * crosses, so it's where that reliance gets enforced rather than just
     * documented.
     */
    private fun requireStampedSnapshots(template: WorkoutTemplate) {
        template.templateExercises.forEach { templateExercise ->
            check(templateExercise.exerciseName.isNotBlank()) {
                "TemplateExercise with exerciseId=${templateExercise.exerciseId} has an unstamped exercise snapshot (blank exerciseName). StampTemplateExerciseSnapshotsUseCase must run before persistence."
            }
        }
    }
}
