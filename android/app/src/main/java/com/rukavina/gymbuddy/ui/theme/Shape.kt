package com.rukavina.gymbuddy.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** `radius.*` from design/tokens.json, in dp. */
internal object RadiusTokens {
    const val EXTRA_SMALL = 8
    const val SMALL = 12
    const val MEDIUM = 16
    const val LARGE = 20
    const val EXTRA_LARGE = 28
    /** 999 in Figma: fully round. Use [CircleShape]. */
    const val FULL = 999
}

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(RadiusTokens.EXTRA_SMALL.dp),
    small = RoundedCornerShape(RadiusTokens.SMALL.dp),
    medium = RoundedCornerShape(RadiusTokens.MEDIUM.dp),
    large = RoundedCornerShape(RadiusTokens.LARGE.dp),
    extraLarge = RoundedCornerShape(RadiusTokens.EXTRA_LARGE.dp),
)

/** `radius.full`. */
val FullShape = CircleShape

/** Sheets round the top corners only. */
val SheetShape = RoundedCornerShape(
    topStart = RadiusTokens.EXTRA_LARGE.dp,
    topEnd = RadiusTokens.EXTRA_LARGE.dp,
)
