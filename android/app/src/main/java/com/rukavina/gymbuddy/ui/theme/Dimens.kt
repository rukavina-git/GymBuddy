package com.rukavina.gymbuddy.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * `space.*`, `size.*` and `stroke.*` from design/tokens.json, in dp.
 * Read via `GymBuddyTheme.dimens`. Raw sizes that have no token yet
 * (DESIGN.md §4) are used as drawn, in their component.
 */
@Immutable
data class Dimens(
    // space
    val hairline: Dp = 4.dp,
    val stackTight: Dp = 8.dp,
    val stack: Dp = 12.dp,
    val group: Dp = 16.dp,
    val insetScreen: Dp = 16.dp,
    val insetCard: Dp = 16.dp,
    val insetDialog: Dp = 24.dp,
    val section: Dp = 24.dp,
    // size
    val chip: Dp = 36.dp,
    val control: Dp = 56.dp,
    val icon: Dp = 24.dp,
    val iconSmall: Dp = 20.dp,
    val key: Dp = 56.dp,
    val row: Dp = 72.dp,
    val rowSet: Dp = 48.dp,
    val stepperWidth: Dp = 60.dp,
    /** = M3 minimumInteractiveComponentSize. */
    val target: Dp = 48.dp,
    // stroke
    val strokeDivider: Dp = 1.dp,
    val strokeOutline: Dp = 1.5.dp,
    val strokeEmphasis: Dp = 2.dp,
    val strokeIcon: Dp = 2.dp,
    val strokeChart: Dp = 2.5.dp,
    val strokeMark: Dp = 3.dp,
)

internal val LocalDimens = staticCompositionLocalOf { Dimens() }
