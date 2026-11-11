package com.rukavina.gymbuddy.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Soft Dark colour tokens, copied from design/tokens.json (`colour.*`).
 * The JSON wins; TokenParityTest fails if these drift from it.
 * Meaning and use: design/colors.md.
 */
object ColorTokens {
    // brand
    val Lime = Color(0xFFD4FF24)
    val Cobalt = Color(0xFF4D86F0)

    // surface (darkest to lightest)
    val Ground = Color(0xFF121418)
    val Surface = Color(0xFF1C1F26)
    val SurfaceRaised = Color(0xFF262A33)
    val SurfaceHigh = Color(0xFF323743)

    // text
    val Text = Color(0xFFF3F4F6)
    val TextSoft = Color(0xFFC9CDD4)
    val Muted = Color(0xFF9AA1AD)
    val OnLime = Ground

    // line
    val Outline = Color(0xFF717886)
    val Disabled = Color(0xFF5E6571)

    // tint
    val LimePressed = Color(0xFFB8DE1F)
    val LimeTint = Color(0xFF2E3A1A)
    val CobaltTint = Color(0x404D86F0)

    // danger
    val Danger = Color(0xFFFF7A7A)
    val DangerContainer = Color(0xFF2B1D20)
    val DangerSwipe = Color(0xFF5C1F1F)

    // overlay
    val Scrim = Color(0x99000000)
    val Shadow = Color(0x80000000)
    val Ripple = Color(0xFFFFFFFF)
}

/** M3 slots per design/DESIGN.md §3 colour map. Dark only in v1. */
internal val SoftDarkColorScheme = darkColorScheme(
    primary = ColorTokens.Lime,
    onPrimary = ColorTokens.OnLime,
    primaryContainer = ColorTokens.LimeTint,
    onPrimaryContainer = ColorTokens.Lime,
    secondary = ColorTokens.Cobalt,
    onSecondary = ColorTokens.Text,
    secondaryContainer = ColorTokens.CobaltTint,
    onSecondaryContainer = ColorTokens.Text,
    tertiary = ColorTokens.Cobalt,
    onTertiary = ColorTokens.Text,
    tertiaryContainer = ColorTokens.CobaltTint,
    onTertiaryContainer = ColorTokens.Text,
    background = ColorTokens.Ground,
    onBackground = ColorTokens.Text,
    surface = ColorTokens.Ground,
    onSurface = ColorTokens.Text,
    surfaceVariant = ColorTokens.SurfaceRaised,
    onSurfaceVariant = ColorTokens.Muted,
    surfaceTint = Color.Transparent,
    surfaceBright = ColorTokens.SurfaceHigh,
    surfaceDim = ColorTokens.Ground,
    surfaceContainerLowest = ColorTokens.Ground,
    surfaceContainerLow = ColorTokens.Surface,
    surfaceContainer = ColorTokens.Surface,
    surfaceContainerHigh = ColorTokens.SurfaceRaised,
    surfaceContainerHighest = ColorTokens.SurfaceHigh,
    inverseSurface = ColorTokens.Text,
    inverseOnSurface = ColorTokens.Ground,
    inversePrimary = ColorTokens.Cobalt,
    outline = ColorTokens.Outline,
    outlineVariant = ColorTokens.SurfaceHigh,
    error = ColorTokens.Danger,
    onError = ColorTokens.Ground,
    errorContainer = ColorTokens.DangerContainer,
    onErrorContainer = ColorTokens.Danger,
    scrim = ColorTokens.Scrim,
)

/** Brand colours with no M3 slot. Read via `GymBuddyTheme.colors`. */
@Immutable
data class GymBuddyColors(
    val cobalt: Color = ColorTokens.Cobalt,
    val cobaltTint: Color = ColorTokens.CobaltTint,
    val textSoft: Color = ColorTokens.TextSoft,
    val disabled: Color = ColorTokens.Disabled,
    val limePressed: Color = ColorTokens.LimePressed,
    val limeTint: Color = ColorTokens.LimeTint,
    val dangerSwipe: Color = ColorTokens.DangerSwipe,
    val shadow: Color = ColorTokens.Shadow,
    val ripple: Color = ColorTokens.Ripple,
)

internal val LocalGymBuddyColors = staticCompositionLocalOf { GymBuddyColors() }
