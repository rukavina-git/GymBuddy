package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.api.dto.WorkoutSessionDto
import com.rukavina.gymbuddy.persistence.WorkoutSessionPullRepository
import org.springframework.stereotype.Service

/** Read side of the WorkoutSession aggregate for pull - the counterpart to WorkoutSessionSyncService's write side. */
@Service
class WorkoutSessionPullService(private val repository: WorkoutSessionPullRepository) {
    fun findAllByIds(uid: String, ids: Collection<String>): List<WorkoutSessionDto> = repository.findAllByIds(uid, ids)
}
