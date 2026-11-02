package com.rukavina.gymbuddy.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rukavina.gymbuddy.domain.sync.SyncOutcome
import com.rukavina.gymbuddy.domain.sync.SyncReason
import com.rukavina.gymbuddy.domain.sync.SyncRequester
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Pushes a non-empty outbox once a network is there, even if the app was
 * swiped away in the meantime. Enqueued by [SyncTriggers.observeOutbox].
 * A failed cycle is retried with WorkManager's exponential backoff.
 */
@HiltWorker
class PushOutboxWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val requester: SyncRequester,
    private val auth: AuthSession
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = when {
        // Nothing to push while signed out; the outbox waits for sign-in.
        auth.currentUid == null -> Result.success()
        requester.syncNow(SyncReason.OUTBOX_PENDING) == SyncOutcome.FAILED -> Result.retry()
        else -> Result.success()
    }
}
