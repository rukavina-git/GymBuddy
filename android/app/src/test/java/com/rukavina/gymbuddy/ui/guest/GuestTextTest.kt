package com.rukavina.gymbuddy.ui.guest

import com.rukavina.gymbuddy.data.sync.GuestMergeReceipt
import com.rukavina.gymbuddy.data.sync.GuestMergeState
import com.rukavina.gymbuddy.data.sync.LocalDataCounts
import org.junit.Assert.assertEquals
import org.junit.Test

class GuestTextTest {

    private fun counts(w: Int = 0, t: Int = 0, e: Int = 0) = LocalDataCounts(w, t, e)

    @Test
    fun countsTextJoinsNonZeroParts() {
        assertEquals("changes", countsText(counts()))
        assertEquals("1 workout", countsText(counts(w = 1)))
        assertEquals("2 templates", countsText(counts(t = 2)))
        assertEquals("5 workouts and 1 exercise", countsText(counts(w = 5, e = 1)))
        assertEquals("5 workouts, 1 template and 2 exercises", countsText(counts(5, 1, 2)))
    }

    @Test
    fun askTextWithAccountWorkouts() {
        val state = GuestMergeState.Asking("uid", "a@b.com", 3, counts(w = 2))
        assertEquals(
            "a@b.com already has 3 workouts. Add the 2 workouts from this phone to it? Nothing is deleted.",
            askText(state)
        )
    }

    @Test
    fun askTextWhenAccountUnknown() {
        val state = GuestMergeState.Asking("uid", null, 0, counts(t = 1))
        assertEquals(
            "We couldn't check what Your account already has. Add the 1 template from this phone to it? Nothing is deleted.",
            askText(state)
        )
    }

    @Test
    fun receiptText() {
        assertEquals(
            "Added your changes from this phone.",
            receiptText(GuestMergeReceipt(counts(), keptAccountProfile = false))
        )
        assertEquals(
            "Added 1 workout from this phone. Kept your account's profile.",
            receiptText(GuestMergeReceipt(counts(w = 1), keptAccountProfile = true))
        )
    }

    @Test
    fun maskEmail() {
        assertEquals("k…@gmail.com", maskEmail("karlo@gmail.com"))
        assertEquals("k@gmail.com", maskEmail("k@gmail.com"))
        assertEquals("@x.com", maskEmail("@x.com"))
        assertEquals("no-at-sign", maskEmail("no-at-sign"))
    }
}
