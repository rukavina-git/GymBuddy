package com.rukavina.gymbuddy.domain.repository

import com.rukavina.gymbuddy.domain.model.ActiveWorkoutDraft

/** The single in-progress workout draft on this device. Local only. */
interface ActiveWorkoutDraftRepository {
    /** The stored draft, or null if there is none or it can't be read. */
    suspend fun load(): ActiveWorkoutDraft?

    /** Replaces the stored draft. */
    suspend fun save(draft: ActiveWorkoutDraft)

    suspend fun clear()
}
