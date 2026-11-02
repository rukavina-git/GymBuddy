package com.rukavina.gymbuddy.data.repository

import android.util.Log
import com.rukavina.gymbuddy.data.local.dao.ActiveWorkoutDraftDao
import com.rukavina.gymbuddy.data.local.entity.ActiveWorkoutDraftEntity
import com.rukavina.gymbuddy.domain.model.ActiveWorkoutDraft
import com.rukavina.gymbuddy.domain.repository.ActiveWorkoutDraftRepository
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.Clock

/**
 * Own Moshi instance, not the generated API client's: the draft is a
 * local format and must not change when the OpenAPI client does.
 */
class ActiveWorkoutDraftRepositoryImpl(
    private val dao: ActiveWorkoutDraftDao,
    private val clock: Clock
) : ActiveWorkoutDraftRepository {

    private val adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(ActiveWorkoutDraft::class.java)

    override suspend fun load(): ActiveWorkoutDraft? {
        val row = dao.get() ?: return null
        return try {
            adapter.fromJson(row.payloadJson)
        } catch (e: JsonDataException) {
            dropUnreadable(e)
        } catch (e: JsonEncodingException) {
            dropUnreadable(e)
        }
    }

    override suspend fun save(draft: ActiveWorkoutDraft) {
        dao.upsert(
            ActiveWorkoutDraftEntity(
                payloadJson = adapter.toJson(draft),
                updatedAt = clock.millis()
            )
        )
    }

    override suspend fun clear() = dao.delete()

    // A draft from an incompatible older build can't be resumed; keeping
    // it would only fail again on every start.
    private suspend fun dropUnreadable(e: Exception): ActiveWorkoutDraft? {
        Log.w(TAG, "Dropping unreadable workout draft", e)
        dao.delete()
        return null
    }

    private companion object {
        const val TAG = "ActiveWorkoutDraft"
    }
}
