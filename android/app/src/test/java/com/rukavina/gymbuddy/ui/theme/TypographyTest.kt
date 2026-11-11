package com.rukavina.gymbuddy.ui.theme

import androidx.compose.ui.text.TextStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** tnum sits exactly on the styles DESIGN.md §3 names. */
class TypographyTest {

    private val styles: Map<String, TextStyle> = mapOf(
        "displayLarge" to Typography.displayLarge,
        "displayMedium" to Typography.displayMedium,
        "headlineLarge" to Typography.headlineLarge,
        "headlineMedium" to Typography.headlineMedium,
        "headlineSmall" to Typography.headlineSmall,
        "titleLarge" to Typography.titleLarge,
        "titleMedium" to Typography.titleMedium,
        "titleSmall" to Typography.titleSmall,
        "bodyLarge" to Typography.bodyLarge,
        "bodyMedium" to Typography.bodyMedium,
        "bodySmall" to Typography.bodySmall,
        "labelLarge" to Typography.labelLarge,
        "labelMedium" to Typography.labelMedium,
        "labelSmall" to Typography.labelSmall,
        "statValue" to GymBuddyType().statValue,
        "tag" to GymBuddyType().tag,
    )

    private val tabular = setOf(
        "displayLarge", "displayMedium", "titleSmall", "bodySmall", "labelSmall", "statValue",
    )

    @Test
    fun tabularFiguresOnlyWhereNamed() {
        styles.forEach { (name, style) ->
            if (name in tabular) assertEquals(name, "tnum", style.fontFeatureSettings)
            else assertNull(name, style.fontFeatureSettings)
        }
    }

    @Test
    fun everyStyleUsesFigtree() {
        styles.forEach { (name, style) -> assertEquals(name, Figtree, style.fontFamily) }
    }
}
