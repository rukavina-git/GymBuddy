package com.rukavina.gymbuddy.data.feedback

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Sends queued feedback once a network is there, even after the app was
 * swiped away. Retries with exponential backoff until the queue is empty.
 */
@HiltWorker
class FeedbackWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val outbox: FeedbackOutbox,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = if (outbox.flush()) Result.success() else Result.retry()

    companion object {
        const val WORK_NAME = "feedback-send"
        private const val BACKOFF_SECONDS = 60L

        /** One pending run is enough: it sends the whole queue. */
        fun enqueue(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<FeedbackWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .build()
            workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
