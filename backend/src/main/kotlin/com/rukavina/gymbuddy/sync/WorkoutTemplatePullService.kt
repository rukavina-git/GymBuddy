package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.api.dto.WorkoutTemplateDto
import com.rukavina.gymbuddy.persistence.WorkoutTemplatePullRepository
import org.springframework.stereotype.Service

/** Read side of the WorkoutTemplate aggregate for pull - the counterpart to WorkoutTemplateSyncService's write side. */
@Service
class WorkoutTemplatePullService(private val repository: WorkoutTemplatePullRepository) {
    fun findAllByIds(uid: String, ids: Collection<String>): List<WorkoutTemplateDto> = repository.findAllByIds(uid, ids)
}
