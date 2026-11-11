package com.rukavina.gymbuddy.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import com.rukavina.gymbuddy.ui.theme.Durations
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.theme.PRESS_SCALE
import com.rukavina.gymbuddy.ui.theme.motionTween

/**
 * Focus: a 2dp cobalt ring just outside the control (components.md).
 * Drawn, not laid out, so it never changes the control's size.
 */
@Composable
fun Modifier.focusRing(interaction: InteractionSource, shape: Shape): Modifier {
    val focused by interaction.collectIsFocusedAsState()
    val color = GymBuddyTheme.colors.cobalt
    val width = GymBuddyTheme.dimens.strokeEmphasis
    return drawWithContent {
        drawContent()
        if (focused) {
            val w = width.toPx()
            val outline = shape.createOutline(Size(size.width + 2 * w, size.height + 2 * w), layoutDirection, this)
            translate(-w, -w) { drawOutline(outline, color, style = Stroke(w)) }
        }
    }
}

/**
 * Press shrink (motion.md rule 7, row 1): buttons and keys only.
 * In `short2`, out `short3`, both `std`; the ripple stays.
 */
@Composable
fun Modifier.pressScale(interaction: InteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESS_SCALE else 1f,
        animationSpec = motionTween(if (pressed) Durations.SHORT2 else Durations.SHORT3),
        label = "pressScale",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}
