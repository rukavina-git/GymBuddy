package com.rukavina.gymbuddy.review

import android.app.Activity
import android.app.Application
import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.testing.FakeReviewManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class StoreReviewTest {
    private val activity: Activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    private val prefs = activity.getSharedPreferences("store_review_test", Context.MODE_PRIVATE)

    /** Counts requests; delegates to Play's fake so the task API is real. */
    private class CountingManager(private val fake: FakeReviewManager) : ReviewManager by fake {
        var requests = 0
        override fun requestReviewFlow(): Task<ReviewInfo> = fake.requestReviewFlow().also { requests++ }
    }

    private val manager = CountingManager(FakeReviewManager(activity))
    private val review = StoreReview(prefs, manager)

    private fun finish(times: Int) = repeat(times) { review.onWorkoutFinished(activity) }

    @Test
    fun `asks after the 3rd and the 10th workout, then never`() {
        finish(2)
        assertEquals(0, manager.requests)
        finish(1)
        assertEquals(1, manager.requests)
        finish(6)
        assertEquals(1, manager.requests)
        finish(1)
        assertEquals(2, manager.requests)
        finish(50)
        assertEquals(2, manager.requests)
    }

    @Test
    fun `policy`() {
        assertFalse(ReviewPolicy.shouldAsk(finished = 2, asks = 0))
        assertTrue(ReviewPolicy.shouldAsk(finished = 3, asks = 0))
        assertFalse(ReviewPolicy.shouldAsk(finished = 9, asks = 1))
        assertTrue(ReviewPolicy.shouldAsk(finished = 10, asks = 1))
        assertFalse(ReviewPolicy.shouldAsk(finished = 100, asks = 2))
    }

    @Test
    fun `create builds a working instance`() {
        StoreReview.create(activity).onWorkoutFinished(activity)
        assertEquals(1, activity.getSharedPreferences("store_review", Context.MODE_PRIVATE).getInt("finished_workouts", 0))
    }
}
