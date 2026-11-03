package com.rukavina.gymbuddy.ui.theme

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

// Screen transitions shared by the app frame, the start-up graph and setup.

// motion.md #14: old fades out, then the new one fades in, medium2 total.
val fadeThroughOut = fadeOut(motionTween(Durations.MEDIUM2 / 3))
val fadeThroughIn = fadeIn(tween(Durations.MEDIUM2 * 2 / 3, delayMillis = Durations.MEDIUM2 / 3, easing = Easings.Standard))

// motion.md #15: shared axis X; predictive back scrubs these with the gesture.
fun sharedAxisIn(forward: Boolean) =
    slideInHorizontally(motionTween(Durations.MEDIUM2, Easings.Emphasized)) { w -> if (forward) w / SHARED_AXIS_SHIFT else -w / SHARED_AXIS_SHIFT } +
        fadeIn(motionTween(Durations.MEDIUM2, Easings.Emphasized))

fun sharedAxisOut(forward: Boolean) =
    slideOutHorizontally(motionTween(Durations.MEDIUM2, Easings.Emphasized)) { w -> if (forward) -w / SHARED_AXIS_SHIFT else w / SHARED_AXIS_SHIFT } +
        fadeOut(motionTween(Durations.MEDIUM2, Easings.Emphasized))

/** Screens move a tenth of the width, like M3 shared axis (30dp on a phone). */
private const val SHARED_AXIS_SHIFT = 10
