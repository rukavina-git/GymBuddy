package com.rukavina.gymbuddy.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rukavina.gymbuddy.domain.sync.SyncReason
import com.rukavina.gymbuddy.domain.sync.SyncRequester
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** The periodic (~6h) trigger. A failed cycle is retried with WorkManager's backoff. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val requester: SyncRequester,
    private val auth: AuthSession
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = when {
        // Nothing to sync while signed out; don't burn retries on it.
        auth.currentUid == null -> Result.success()
        requester.syncNow(SyncReason.PERIODIC) -> Result.success()
        else -> Result.retry()
    }
}
