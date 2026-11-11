package com.rukavina.gymbuddy.ui.theme

import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.TransformOrigin
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

// motion.md #10: the workout shrinks into the live bar (⌄) and grows back
// from it. Stand-in for a container transform: the bar lives in the frame's
// bottom bar, outside the NavHost, so the two can't share bounds.
private val LiveBarOrigin = TransformOrigin(0.5f, 1f)
val collapseIntoLiveBar =
    scaleOut(motionTween(Durations.LONG2, Easings.Emphasized), targetScale = LIVE_BAR_SCALE, transformOrigin = LiveBarOrigin) +
        fadeOut(motionTween(Durations.LONG2, Easings.Emphasized))
val growFromLiveBar =
    scaleIn(motionTween(Durations.LONG2, Easings.Emphasized), initialScale = LIVE_BAR_SCALE, transformOrigin = LiveBarOrigin) +
        fadeIn(motionTween(Durations.LONG2, Easings.Emphasized))
private const val LIVE_BAR_SCALE = 0.1f
