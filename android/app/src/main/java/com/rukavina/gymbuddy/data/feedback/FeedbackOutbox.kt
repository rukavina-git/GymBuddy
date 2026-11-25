package com.rukavina.gymbuddy.data.feedback

import android.util.Log
import com.rukavina.gymbuddy.data.remote.generated.models.FeedbackRequest
import com.rukavina.gymbuddy.domain.id.IdGenerator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.time.Clock
import java.util.UUID

/** Max message length, as in the spec (`FeedbackRequest.message`). */
const val FEEDBACK_MAX_LENGTH = 2000

/** App, Android and phone, sent along with each message. */
data class FeedbackDevice(val appVersion: String, val osVersion: String, val deviceModel: String)

/** Sends one message; returns the HTTP status, throws [IOException] when there was no answer. */
fun interface FeedbackRemote {
    /** [uid]: send with that account's token, but only if it is still the one signed in. */
    suspend fun send(request: FeedbackRequest, uid: String?): Int
}

enum class FeedbackSubmitResult {
    /** The server has it. */
    SENT,

    /** Kept on the phone; [FeedbackWorker] sends it once online. */
    QUEUED,
}

/**
 * In-app feedback (Plan 10 step 06b). Send stores the message first, so
 * it is never lost, then tries to post it right away. Whatever doesn't
 * go out is left for [FeedbackWorker] (network constraint, backoff).
 *
 * A row leaves the queue when the server answers 2xx, or a 4xx other
 * than 429 (retrying can't fix a rejected message). No answer, 429 and
 * 5xx keep it and stop the run.
 */
class FeedbackOutbox(
    private val dao: () -> PendingFeedbackDao,
    private val remote: FeedbackRemote,
    private val ids: IdGenerator,
    private val clock: Clock,
    private val device: FeedbackDevice,
    private val currentUid: () -> String?,
    private val scheduleSend: () -> Unit,
    private val immediateTimeoutMs: Long = IMMEDIATE_TIMEOUT_MS,
) {
    private val lock = Mutex()

    suspend fun submit(message: String): FeedbackSubmitResult {
        val text = message.trim()
        require(text.isNotEmpty() && text.length <= FEEDBACK_MAX_LENGTH) { "message must be 1..$FEEDBACK_MAX_LENGTH chars" }
        val row = PendingFeedbackEntity(
            id = ids.newId(),
            message = text,
            uid = currentUid(),
            appVersion = device.appVersion,
            osVersion = device.osVersion,
            deviceModel = device.deviceModel,
            createdAt = clock.millis(),
        )
        dao().insert(row)
        // A slow network shouldn't keep the user waiting: past the timeout
        // the worker takes over. A post cut off after the server stored it
        // is harmless: the retry reuses the id and the server dedupes.
        withTimeoutOrNull(immediateTimeoutMs) { flush() }
        if (dao().exists(row.id) == 0) return FeedbackSubmitResult.SENT
        scheduleSend()
        return FeedbackSubmitResult.QUEUED
    }

    /** Posts every queued message, oldest first. True when the queue is empty. */
    suspend fun flush(): Boolean = lock.withLock {
        for (row in dao().all()) {
            val status = try {
                remote.send(row.toRequest(), row.uid)
            } catch (e: IOException) {
                Log.i(TAG, "feedback not sent: ${e.javaClass.simpleName}")
                return@withLock false
            }
            when {
                status in 200..299 -> dao().delete(row.id)
                status == 429 || status >= 500 -> {
                    Log.i(TAG, "feedback not sent: HTTP $status")
                    return@withLock false
                }
                else -> {
                    // 400, 413 ...: the same body would fail again.
                    Log.w(TAG, "feedback rejected: HTTP $status, dropped")
                    dao().delete(row.id)
                }
            }
        }
        true
    }

    /** Account deleted: its unsent messages go too (they'd be linked to it). */
    suspend fun deleteForAccount(uid: String) = dao().deleteForUid(uid)

    private fun PendingFeedbackEntity.toRequest() = FeedbackRequest(
        id = UUID.fromString(id),
        message = message,
        createdAt = createdAt,
        appVersion = appVersion,
        osVersion = osVersion,
        deviceModel = deviceModel,
    )

    companion object {
        private const val TAG = "FeedbackOutbox"
        const val IMMEDIATE_TIMEOUT_MS = 8_000L
    }
}
