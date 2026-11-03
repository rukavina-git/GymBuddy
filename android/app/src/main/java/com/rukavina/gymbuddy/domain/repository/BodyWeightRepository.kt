package com.rukavina.gymbuddy.domain.repository

import com.rukavina.gymbuddy.domain.model.BodyWeightEntry
import kotlinx.coroutines.flow.Flow

interface BodyWeightRepository {
    /** Live entries, newest measurement first. */
    fun observeEntries(): Flow<List<BodyWeightEntry>>

    /** Adds or edits an entry. The newest one also becomes the profile's weight. */
    suspend fun save(entry: BodyWeightEntry)

    /** Soft delete (tombstone), synced like any other change. */
    suspend fun delete(id: String)
}
