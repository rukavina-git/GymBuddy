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

    override suspend fun save(draft: ActiveWorkoutDraft) {
        this.draft = draft
        saveCount++
    }

    override suspend fun clear() {
        draft = null
    }
}
