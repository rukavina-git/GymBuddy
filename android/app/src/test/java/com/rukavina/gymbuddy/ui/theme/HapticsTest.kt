package com.rukavina.gymbuddy.ui.theme

import android.os.Build
import android.view.HapticFeedbackConstants as H
import org.junit.Assert.assertEquals
import org.junit.Test

/** The haptics map in design/motion.md, with its fallbacks (minSdk 28). */
class HapticsTest {

    private val min = 28
    private val r = Build.VERSION_CODES.R
    private val u = Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    private fun check(h: Haptic, below: Int?, from: Int?, api: Int) {
        assertEquals("$h below $api", below, hapticConstant(h, api - 1))
        assertEquals("$h at $api", from, hapticConstant(h, api))
        assertEquals("$h at minSdk", if (min < api) below else from, hapticConstant(h, min))
    }

    @Test fun confirm() = check(Haptic.Confirm, H.VIRTUAL_KEY, H.CONFIRM, r)
    @Test fun reject() = check(Haptic.Reject, null, H.REJECT, r)
    @Test fun swipeThreshold() =
        check(Haptic.SwipeThreshold, H.CONTEXT_CLICK, H.GESTURE_THRESHOLD_ACTIVATE, u)
    @Test fun dragStart() = check(Haptic.DragStart, H.LONG_PRESS, H.DRAG_START, u)
    @Test fun switchOn() = check(Haptic.SwitchOn, H.CLOCK_TICK, H.TOGGLE_ON, u)
    @Test fun switchOff() = check(Haptic.SwitchOff, H.CLOCK_TICK, H.TOGGLE_OFF, u)

    @Test
    fun noFallbackNeeded() {
        listOf(min, r, u).forEach { api ->
            assertEquals(H.CLOCK_TICK, hapticConstant(Haptic.StepperTick, api))
            assertEquals(H.KEYBOARD_TAP, hapticConstant(Haptic.KeypadKey, api))
        }
    }
}
