package com.rukavina.gymbuddy.review

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Play's in-app rating dialog (0.2.3+). Asked after the 3rd finished
 * workout, once more after the 10th, then never. Play adds its own
 * quota and may show nothing. Never mid-workout and never behind a
 * "do you like the app?" question (Play policy).
 */
class StoreReview(
    private val prefs: SharedPreferences,
    private val manager: ReviewManager,
) {
    /** Call when a finished workout's summary is closed with Done. */
    fun onWorkoutFinished(activity: Activity) {
        val finished = prefs.getInt(KEY_FINISHED, 0) + 1
        val asks = prefs.getInt(KEY_ASKS, 0)
        val ask = ReviewPolicy.shouldAsk(finished, asks)
        prefs.edit {
            putInt(KEY_FINISHED, finished)
            if (ask) putInt(KEY_ASKS, asks + 1)
        }
        if (!ask) return
        manager.requestReviewFlow()
            .addOnSuccessListener { info -> manager.launchReviewFlow(activity, info) }
            .addOnFailureListener { Log.d(TAG, "review unavailable: ${it.message}") }
    }

    companion object {
        private const val TAG = "StoreReview"
        private const val PREFS = "store_review"
        private const val KEY_FINISHED = "finished_workouts"
        private const val KEY_ASKS = "asks"

        fun create(context: Context) = StoreReview(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE),
            ReviewManagerFactory.create(context),
        )
    }
}

/** When to ask: the 3rd finished workout, then the 10th, at most twice. */
internal object ReviewPolicy {
    private val ASK_AT = listOf(3, 10)

    fun shouldAsk(finished: Int, asks: Int): Boolean =
        asks < ASK_AT.size && finished >= ASK_AT[asks]
}
