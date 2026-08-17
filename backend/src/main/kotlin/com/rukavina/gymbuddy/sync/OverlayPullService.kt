package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.api.dto.UserExerciseStateDto
import com.rukavina.gymbuddy.api.dto.UserTemplateStateDto
import com.rukavina.gymbuddy.persistence.OverlayPullRepository
import org.springframework.stereotype.Service

/** Read side of both overlay tables for pull - the counterpart to OverlaySyncService's write side. */
@Service
class OverlayPullService(private val repository: OverlayPullRepository) {
    fun findAllExerciseStatesByIds(uid: String, ids: Collection<String>): List<UserExerciseStateDto> =
        repository.findAllExerciseStatesByIds(uid, ids)

    fun findAllTemplateStatesByIds(uid: String, ids: Collection<String>): List<UserTemplateStateDto> =
        repository.findAllTemplateStatesByIds(uid, ids)
}
