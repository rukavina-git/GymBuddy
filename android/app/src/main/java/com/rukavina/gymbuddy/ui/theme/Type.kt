package com.rukavina.gymbuddy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rukavina.gymbuddy.R

/** Figtree, bundled (OFL 1.1, assets/licenses/figtree-OFL.txt). */
val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
)

/** Tabular figures, so digits don't jump as numbers change. */
internal const val TABULAR_FIGURES = "tnum"

/**
 * One `type.*` role from design/tokens.json. Sizes and line heights in
 * sp, tracking in sp (Figma float noise rounded).
 */
internal data class TypeToken(
    val size: Int,
    val lineHeight: Int,
    val weight: Int,
    val tracking: Double,
    val tabular: Boolean = false,
)

/** Copied from tokens.json; TokenParityTest checks it. No display-small. */
internal val TypeTokens: Map<String, TypeToken> = mapOf(
    "display-large" to TypeToken(52, 56, 700, -0.5, tabular = true),
    "display-medium" to TypeToken(44, 48, 700, -0.25, tabular = true),
    "headline-large" to TypeToken(32, 40, 700, 0.0),
    "headline-medium" to TypeToken(28, 36, 700, 0.0),
    "headline-small" to TypeToken(24, 32, 600, 0.0),
    "title-large" to TypeToken(20, 28, 600, 0.0),
    "title-medium" to TypeToken(18, 24, 600, 0.0),
    "title-small" to TypeToken(16, 22, 600, 0.0, tabular = true),
    "body-large" to TypeToken(16, 24, 400, 0.1),
    "body-medium" to TypeToken(15, 22, 400, 0.1),
    "body-small" to TypeToken(14, 20, 400, 0.15, tabular = true),
    "label-large" to TypeToken(16, 20, 600, 0.1),
    "label-medium" to TypeToken(14, 20, 500, 0.15),
    "label-small" to TypeToken(12, 16, 500, 0.25, tabular = true),
    "stat-value" to TypeToken(22, 28, 700, 0.0, tabular = true),
    "tag" to TypeToken(12, 16, 700, 0.4),
)

internal fun TypeToken.toStyle() = TextStyle(
    fontFamily = Figtree,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    fontFeatureSettings = if (tabular) TABULAR_FIGURES else null,
)

private fun style(name: String) = TypeTokens.getValue(name).toStyle()

val Typography = Typography(
    displayLarge = style("display-large"),
    displayMedium = style("display-medium"),
    headlineLarge = style("headline-large"),
    headlineMedium = style("headline-medium"),
    headlineSmall = style("headline-small"),
    titleLarge = style("title-large"),
    titleMedium = style("title-medium"),
    titleSmall = style("title-small"),
    bodyLarge = style("body-large"),
    bodyMedium = style("body-medium"),
    bodySmall = style("body-small"),
    labelLarge = style("label-large"),
    labelMedium = style("label-medium"),
    labelSmall = style("label-small"),
    // displaySmall has no token (DESIGN.md §3): don't use it.
)

/** The two custom roles. Read via `GymBuddyTheme.type`. */
@Immutable
data class GymBuddyType(
    /** Numbers on cards and stat tiles. */
    val statValue: TextStyle = style("stat-value"),
    /** PR tags. */
    val tag: TextStyle = style("tag"),
)

internal val LocalGymBuddyType = staticCompositionLocalOf { GymBuddyType() }
