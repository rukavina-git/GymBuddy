package com.rukavina.gymbuddy.ui.theme

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * The haptics map in design/motion.md. Haptics only answer a user
 * action and follow the phone's touch-feedback setting, so everything
 * goes through [View.performHapticFeedback]. The rest buzz (Vibrator)
 * is not here; it comes with the active workout (7.6).
 */
enum class Haptic {
    /** Done on a set (tap or swipe released), Finish workout confirmed. */
    Confirm,
    /** Swipe crosses its threshold, either way. */
    SwipeThreshold,
    /** A hold hits floor/ceiling, invalid keypad Next. None below API 30. */
    Reject,
    /** Set or workout couldn't save: REJECT, LONG_PRESS below API 30 (motion.md). */
    SaveFailed,
    /** − / + press: first tick only, none per repeat. */
    StepperTick,
    /** Keypad key. */
    KeypadKey,
    /** Long-press a set; lift a drag handle. */
    DragStart,
    SwitchOn,
    SwitchOff,
}

/**
 * The [HapticFeedbackConstants] value for [haptic] on [sdkInt], or null
 * for no feedback (Reject has none below API 30, per motion.md).
 */
internal fun hapticConstant(haptic: Haptic, sdkInt: Int): Int? {
    val r = sdkInt >= Build.VERSION_CODES.R
    val u = sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    return when (haptic) {
        Haptic.Confirm ->
            if (r) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
        Haptic.SwipeThreshold ->
            if (u) HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
            else HapticFeedbackConstants.CONTEXT_CLICK
        Haptic.Reject -> if (r) HapticFeedbackConstants.REJECT else null
        Haptic.SaveFailed -> if (r) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
        Haptic.StepperTick -> HapticFeedbackConstants.CLOCK_TICK
        Haptic.KeypadKey -> HapticFeedbackConstants.KEYBOARD_TAP
        Haptic.DragStart ->
            if (u) HapticFeedbackConstants.DRAG_START else HapticFeedbackConstants.LONG_PRESS
        Haptic.SwitchOn ->
            if (u) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.CLOCK_TICK
        Haptic.SwitchOff ->
            if (u) HapticFeedbackConstants.TOGGLE_OFF else HapticFeedbackConstants.CLOCK_TICK
    }
}

/** Performs [haptic] on this view, respecting the phone's setting. */
fun View.perform(haptic: Haptic) {
    hapticConstant(haptic, Build.VERSION.SDK_INT)?.let { performHapticFeedback(it) }
}

/** Haptics for the current view: `val haptics = rememberHaptics(); haptics(Haptic.Confirm)`. */
@Composable
fun rememberHaptics(): (Haptic) -> Unit {
    val view = LocalView.current
    return remember(view) { { haptic: Haptic -> view.perform(haptic) } }
}
