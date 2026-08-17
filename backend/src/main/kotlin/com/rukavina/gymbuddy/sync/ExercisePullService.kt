package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.api.dto.ExerciseDto
import com.rukavina.gymbuddy.persistence.ExercisePullRepository
import org.springframework.stereotype.Service

/** Read side of custom Exercise rows for pull - the counterpart to ExerciseSyncService's write side. */
@Service
class ExercisePullService(private val repository: ExercisePullRepository) {
    fun findAllByIds(uid: String, ids: Collection<String>): List<ExerciseDto> = repository.findAllByIds(uid, ids)
}
