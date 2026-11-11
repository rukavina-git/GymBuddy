package com.rukavina.gymbuddy.ui.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * design/icons + design/brand (mark, wordmark, app icon) vs res/drawable. Every
 * exported SVG has its converted drawable, and no own-icon drawable
 * exists without an SVG behind it (design/icons.md: re-export, don't
 * hand-edit).
 */
class IconParityTest {

    private val design = File(System.getProperty("gymbuddy.designDir") ?: "../../design")
    private val drawable = File("src/main/res/drawable")

    private fun svgNames(dir: String, prefix: String) =
        File(design, dir).listFiles { f -> f.extension == "svg" }.orEmpty()
            .map { prefix + it.nameWithoutExtension.replace('-', '_') }

    @Test
    fun everySvgHasADrawableAndBack() {
        assertTrue("design dir missing: $design", design.isDirectory)
        val expected = (
            svgNames("icons/ui", "ic_ui_") +
                svgNames("icons/glyphs", "ic_glyph_") +
                svgNames("icons/tabs", "ic_tab_") +
                listOf("brand_mark", "brand_wordmark", "brand_app_icon")
            ).toSortedSet()
        val actual = drawable.listFiles().orEmpty()
            .map { it.nameWithoutExtension }
            .filter { it.startsWith("ic_ui_") || it.startsWith("ic_glyph_") || it.startsWith("ic_tab_") || it.startsWith("brand_") }
            .toSortedSet()
        assertEquals(expected, actual)
        // design/icons.md: 36 UI, 12 glyphs, 10 tab states.
        assertEquals(36 + 12 + 10 + 3, actual.size)
    }
}
