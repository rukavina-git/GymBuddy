package com.rukavina.gymbuddy.testutil

import com.rukavina.gymbuddy.domain.model.ActiveWorkoutDraft
import com.rukavina.gymbuddy.domain.repository.ActiveWorkoutDraftRepository

/** In-memory draft store. Share one instance between two ViewModels to simulate process death. */
class FakeActiveWorkoutDraftRepository(
    var draft: ActiveWorkoutDraft? = null
) : ActiveWorkoutDraftRepository {
    var saveCount = 0
        private set

    override suspend fun load(): ActiveWorkoutDraft? = draft

    /** Every save throws, like a full disk (R17-B1). */
    var failSaves = false

    override suspend fun save(draft: ActiveWorkoutDraft) {
        if (failSaves) throw IllegalStateException("disk full")
        this.draft = draft
        saveCount++
    }

    override suspend fun clear() {
        draft = null
    }
}
