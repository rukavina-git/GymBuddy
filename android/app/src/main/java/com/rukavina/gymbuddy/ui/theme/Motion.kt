package com.rukavina.gymbuddy.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween

/**
 * `motion.*` from design/tokens.json. Durations in ms. Usage per moment:
 * design/motion.md. Rows there without an easing use [Easings.Standard].
 *
 * Android "Remove animations": Compose scales every duration by the
 * animator scale, so at 0 all motion ends at once (motion.md rule 4).
 * No extra code needed.
 */
object Durations {
    const val SHORT1 = 50
    const val SHORT2 = 100
    const val SHORT3 = 150
    const val SHORT4 = 200
    const val MEDIUM1 = 250
    const val MEDIUM2 = 300
    const val MEDIUM3 = 350
    const val MEDIUM4 = 400
    const val LONG1 = 450
    const val LONG2 = 500
    /** The set lands (motion.md #2). */
    const val SET_LANDS = 520
}

object Easings {
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val StandardAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    val StandardDecelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
}

/** Buttons and keys only (motion.md rule 7). Figma's 0.97000003 rounded. */
const val PRESS_SCALE = 0.97f

/** `tween` with a token duration; easing defaults to standard. */
fun <T> motionTween(
    durationMillis: Int,
    easing: Easing = Easings.Standard,
): FiniteAnimationSpec<T> = tween(durationMillis, easing = easing)
