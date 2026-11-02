package com.rukavina.gymbuddy.data.repository

import com.rukavina.gymbuddy.data.local.dao.WorkoutTemplateDao
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.local.entity.OutboxOperation
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.local.mapper.WorkoutTemplateMapper
import com.rukavina.gymbuddy.data.sync.OutboxRecorder
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.SyncState
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

/**
 * Implementation of WorkoutTemplateRepository over the local Room
 * database. Same outbox and revision rules as WorkoutSessionRepositoryImpl;
 * DEFAULT templates are never queued (see ExerciseRepositoryImpl).
 * DAOs come from [DatabaseHolder] per call.
 */
class WorkoutTemplateRepositoryImpl @Inject constructor(
    private val holder: DatabaseHolder,
    private val outbox: OutboxRecorder,
    private val clock: Clock
) : WorkoutTemplateRepository {

    private val workoutTemplateDao: WorkoutTemplateDao get() = holder.db.workoutTemplateDao()

    override fun getAllTemplates(): Flow<List<WorkoutTemplate>> {
        return holder.observe { it.workoutTemplateDao().getAllTemplates() }.map { templatesWithExercises ->
            WorkoutTemplateMapper.toDomainList(templatesWithExercises)
        }
    }

    override suspend fun getTemplateById(id: String): WorkoutTemplate? {
        val templateWithExercises = workoutTemplateDao.getTemplateById(id)
        return templateWithExercises?.let { WorkoutTemplateMapper.toDomain(it) }
    }

    override fun searchTemplates(query: String): Flow<List<WorkoutTemplate>> {
        return holder.observe { it.workoutTemplateDao().searchTemplates(query) }.map { templatesWithExercises ->
            WorkoutTemplateMapper.toDomainList(templatesWithExercises)
        }
    }

    override suspend fun createTemplate(template: WorkoutTemplate) {
        requireStampedSnapshots(template)
        val (templateEntity, exerciseEntities) = WorkoutTemplateMapper.toEntities(template)
        writeTemplate(template) { dao, revision ->
            dao.insertTemplateWithExercises(templateEntity.copy(updatedAt = clock.millis(), revision = revision, syncState = SyncState.PENDING), exerciseEntities)
        }
    }

    override suspend fun updateTemplate(template: WorkoutTemplate) {
        requireStampedSnapshots(template)
        val (templateEntity, exerciseEntities) = WorkoutTemplateMapper.toEntities(template)
        writeTemplate(template) { dao, revision ->
            dao.updateTemplateWithExercises(templateEntity.copy(updatedAt = clock.millis(), revision = revision, syncState = SyncState.PENDING), exerciseEntities)
        }
    }

    /** Runs [write] with the stored revision, queued for sync unless the template is DEFAULT. */
    private suspend fun writeTemplate(template: WorkoutTemplate, write: suspend (WorkoutTemplateDao, revision: Int) -> Unit) {
        if (template.source == EntitySource.DEFAULT) {
            val dao = workoutTemplateDao
            write(dao, dao.getRevision(template.id) ?: 0)
            return
        }
        outbox.record(SyncEntityType.WORKOUT_TEMPLATE, template.id) { db ->
            val dao = db.workoutTemplateDao()
            write(dao, dao.getRevision(template.id) ?: 0)
        }
    }

    override suspend fun deleteTemplate(id: String) {
        val now = clock.millis()
        outbox.record(SyncEntityType.WORKOUT_TEMPLATE, id, OutboxOperation.DELETE) { db ->
            db.workoutTemplateDao().deleteTemplate(id, deletedAt = now, updatedAt = now)
        }
    }

    override suspend fun hideTemplate(id: String) {
        outbox.record(SyncEntityType.USER_TEMPLATE_STATE, id) { db ->
            db.userTemplateStateDao().setHidden(id, true, clock.millis())
        }
    }

    override suspend fun unhideTemplate(id: String) {
        outbox.record(SyncEntityType.USER_TEMPLATE_STATE, id) { db ->
            db.userTemplateStateDao().setHidden(id, false, clock.millis())
        }
    }

    override fun getHiddenTemplates(): Flow<List<WorkoutTemplate>> {
        return holder.observe { it.workoutTemplateDao().getHiddenTemplates() }.map { templatesWithExercises ->
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
