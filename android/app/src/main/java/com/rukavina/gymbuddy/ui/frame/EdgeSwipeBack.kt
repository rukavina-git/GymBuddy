package com.rukavina.gymbuddy.ui.frame

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.ui.theme.Haptic
import com.rukavina.gymbuddy.ui.theme.rememberHaptics
import kotlinx.coroutines.launch
import kotlin.math.abs

/** F5: a drag must start this close to the left edge. */
internal val EdgeWidth = 24.dp

/** Released past this share of the width goes back. */
internal const val COMMIT_FRACTION = 0.35f

/** A rightward fling at least this fast (dp/s) goes back at any distance. */
internal val FlingVelocity = 1000.dp

/** True when a touch at [startX] is inside the left edge zone. */
internal fun startsInEdge(startX: Float, edgePx: Float): Boolean = startX in 0f..edgePx

/** True when a drag released at [dragX] with [velocityX] should go back. */
internal fun commitsBack(dragX: Float, widthPx: Float, velocityX: Float, flingPx: Float): Boolean =
    dragX > 0f && (dragX >= widthPx * COMMIT_FRACTION || velocityX >= flingPx)

/**
 * True when the system already turns a left-edge swipe into Back
 * (gesture navigation). 3-button navigation has no left gesture inset.
 */
@Composable
fun hasSystemBackGesture(): Boolean =
    WindowInsets.systemGestures.getLeft(LocalDensity.current, LocalLayoutDirection.current) > 0

/**
 * F5: iOS-style swipe from the left edge goes back, for 3-button
 * navigation only (with gesture navigation the system does it and this
 * stays out of the way). It goes through the back dispatcher, so every
 * BackHandler (discard dialogs, step back) runs as for the Back button.
 * Does nothing when no back callback is enabled, so it never closes
 * the app.
 */
@Composable
fun EdgeSwipeBack(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    systemBackGesture: Boolean = hasSystemBackGesture(),
    content: @Composable () -> Unit,
) {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val active = enabled && !systemBackGesture && dispatcher != null
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val gesture = if (active && dispatcher != null) {
        Modifier.pointerInput(dispatcher) {
            val edgePx = EdgeWidth.toPx()
            val flingPx = FlingVelocity.toPx()
            awaitEachGesture {
                // Initial pass: the edge zone wins over the screen's own scroll and swipes.
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (!startsInEdge(down.position.x, edgePx) || !dispatcher.hasEnabledCallbacks()) {
                    return@awaitEachGesture
                }
                val slop = viewConfiguration.touchSlop
                val tracker = VelocityTracker()
                tracker.addPosition(down.uptimeMillis, down.position)
                var dragging = false
                var dragX = 0f
                var released = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    tracker.addPosition(change.uptimeMillis, change.position)
                    if (!change.pressed) {
                        if (dragging) change.consume()
                        released = true
                        break
                    }
                    val dx = change.position.x - down.position.x
                    val dy = change.position.y - down.position.y
                    if (!dragging) {
                        if (abs(dy) > slop && abs(dy) > abs(dx)) break
                        if (dx < -slop) break
                        if (dx <= slop) continue
                        dragging = true
                    }
                    change.consume()
                    dragX = dx.coerceAtLeast(0f)
                    scope.launch { offset.snapTo(dragX) }
                }
                if (!dragging) return@awaitEachGesture
                val velocityX = tracker.calculateVelocity().x
                if (released && commitsBack(dragX, size.width.toFloat(), velocityX, flingPx) &&
                    dispatcher.hasEnabledCallbacks()
                ) {
                    haptics(Haptic.SwipeThreshold)
                    dispatcher.onBackPressed()
                    // A guard (discard dialog) keeps the screen: slide it home.
                    scope.launch { offset.animateTo(0f) }
                } else {
                    scope.launch { offset.animateTo(0f) }
                }
            }
        }
    } else {
        Modifier
    }
    Box(modifier.then(gesture).graphicsLayer { translationX = offset.value }) { content() }
}
