package com.rukavina.gymbuddy.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * design/tokens.json is the source of truth (DESIGN.md §3). The Kotlin
 * tokens are copied by hand (Plan 7 step 01, decision 1A); this test
 * fails when they drift. Re-export from Figma, then update ui/theme.
 */
class TokenParityTest {

    private val tokens: Map<String, Any?> = run {
        val path = System.getProperty("gymbuddy.tokensJson")
            ?: error("gymbuddy.tokensJson not set (see app/build.gradle.kts)")
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        Moshi.Builder().build().adapter<Map<String, Any?>>(type).fromJson(File(path).readText())!!
    }

    @Suppress("UNCHECKED_CAST")
    private fun group(path: String): Map<String, Any?> =
        path.split('.').fold(tokens) { node, key -> node[key] as Map<String, Any?> }

    /** Leaf `$value`s under [path], keyed by their dotted sub-path. */
    @Suppress("UNCHECKED_CAST")
    private fun leaves(path: String): Map<String, Any?> {
        val out = linkedMapOf<String, Any?>()
        fun walk(node: Map<String, Any?>, prefix: String) {
            if ("\$value" in node) { out[prefix] = node["\$value"]; return }
            node.filterKeys { !it.startsWith("$") }.forEach { (k, v) ->
                walk(v as Map<String, Any?>, if (prefix.isEmpty()) k else "$prefix.$k")
            }
        }
        walk(group(path), "")
        return out
    }

    private fun resolveColour(value: String): Color {
        val raw = if (value.startsWith("{")) {
            val ref = value.trim('{', '}').removePrefix("colour.")
            leaves("colour").getValue(ref) as String
        } else value
        val hex = raw.removePrefix("#")
        val rgba = if (hex.length == 6) hex + "FF" else hex
        val argb = (rgba.substring(6, 8) + rgba.substring(0, 6)).toLong(16)
        return Color(argb)
    }

    private fun number(v: Any?): Double = (v as Number).toDouble()

    @Test
    fun colours() {
        val kotlin = mapOf(
            "brand.lime" to ColorTokens.Lime,
            "brand.cobalt" to ColorTokens.Cobalt,
            "surface.ground" to ColorTokens.Ground,
            "surface.surface" to ColorTokens.Surface,
            "surface.raised" to ColorTokens.SurfaceRaised,
            "surface.high" to ColorTokens.SurfaceHigh,
            "text.text" to ColorTokens.Text,
            "text.soft" to ColorTokens.TextSoft,
            "text.muted" to ColorTokens.Muted,
            "text.on-lime" to ColorTokens.OnLime,
            "line.outline" to ColorTokens.Outline,
            "line.disabled" to ColorTokens.Disabled,
            "tint.lime-pressed" to ColorTokens.LimePressed,
            "tint.lime-tint" to ColorTokens.LimeTint,
            "tint.cobalt-tint" to ColorTokens.CobaltTint,
            "danger.danger" to ColorTokens.Danger,
            "danger.container" to ColorTokens.DangerContainer,
            "danger.swipe" to ColorTokens.DangerSwipe,
            "overlay.scrim" to ColorTokens.Scrim,
            "overlay.shadow" to ColorTokens.Shadow,
            "overlay.ripple" to ColorTokens.Ripple,
        )
        val json = leaves("colour")
        assertEquals("every colour token mapped", json.keys, kotlin.keys)
        json.forEach { (name, v) ->
            assertEquals(name, resolveColour(v as String), kotlin.getValue(name))
        }
    }

    @Test
    fun typography() {
        val json = group("type").filterKeys { it != "family" && !it.startsWith("$") }
        assertEquals("every type role mapped", json.keys, TypeTokens.keys)
        assertEquals("Figtree", leaves("type")["family"])
        json.keys.forEach { role ->
            val v = leaves("type.$role")
            val k = TypeTokens.getValue(role)
            assertEquals("$role size", number(v["size"]), k.size.toDouble(), 0.0)
            assertEquals("$role line", number(v["line-height"]), k.lineHeight.toDouble(), 0.0)
            assertEquals("$role weight", number(v["weight"]), k.weight.toDouble(), 0.0)
            // Figma stores float32; tokens are rounded (DESIGN.md "Float noise").
            assertEquals("$role tracking", number(v["tracking"]), k.tracking, 1e-6)
        }
    }

    @Test
    fun radius() {
        val kotlin = mapOf(
            "extra-small" to RadiusTokens.EXTRA_SMALL,
            "small" to RadiusTokens.SMALL,
            "medium" to RadiusTokens.MEDIUM,
            "large" to RadiusTokens.LARGE,
            "extra-large" to RadiusTokens.EXTRA_LARGE,
            "full" to RadiusTokens.FULL,
        )
        val json = leaves("radius")
        assertEquals(json.keys, kotlin.keys)
        json.forEach { (k, v) -> assertEquals(k, number(v).roundToInt(), kotlin.getValue(k)) }
    }

    @Test
    fun spaceSizeStroke() {
        val d = Dimens()
        val kotlin: Map<String, Dp> = mapOf(
            "space.hairline" to d.hairline,
            "space.stack-tight" to d.stackTight,
            "space.stack" to d.stack,
            "space.group" to d.group,
            "space.inset-screen" to d.insetScreen,
            "space.inset-card" to d.insetCard,
            "space.inset-dialog" to d.insetDialog,
            "space.section" to d.section,
            "size.chip" to d.chip,
            "size.control" to d.control,
            "size.icon" to d.icon,
            "size.icon-small" to d.iconSmall,
            "size.key" to d.key,
            "size.row" to d.row,
            "size.row-set" to d.rowSet,
            "size.stepper-width" to d.stepperWidth,
            "size.target" to d.target,
            "stroke.divider" to d.strokeDivider,
            "stroke.outline" to d.strokeOutline,
            "stroke.emphasis" to d.strokeEmphasis,
            "stroke.icon" to d.strokeIcon,
            "stroke.chart" to d.strokeChart,
            "stroke.mark" to d.strokeMark,
        )
        val json = listOf("space", "size", "stroke")
            .flatMap { g -> leaves(g).map { (k, v) -> "$g.$k" to v } }.toMap()
        assertEquals("every dimension mapped", json.keys, kotlin.keys)
        json.forEach { (k, v) -> assertEquals(k, number(v).dp, kotlin.getValue(k)) }
    }

    @Test
    fun motion() {
        val durations = mapOf(
            "short1" to Durations.SHORT1, "short2" to Durations.SHORT2,
            "short3" to Durations.SHORT3, "short4" to Durations.SHORT4,
            "medium1" to Durations.MEDIUM1, "medium2" to Durations.MEDIUM2,
            "medium3" to Durations.MEDIUM3, "medium4" to Durations.MEDIUM4,
            "long1" to Durations.LONG1, "long2" to Durations.LONG2,
            "set-lands" to Durations.SET_LANDS,
        )
        val jd = leaves("motion.duration")
        assertEquals(jd.keys, durations.keys)
        jd.forEach { (k, v) -> assertEquals(k, number(v).roundToInt(), durations.getValue(k)) }

        val easings = mapOf(
            "standard" to Easings.Standard,
            "standard-accelerate" to Easings.StandardAccelerate,
            "standard-decelerate" to Easings.StandardDecelerate,
            "emphasized" to Easings.Emphasized,
            "emphasized-accelerate" to Easings.EmphasizedAccelerate,
            "emphasized-decelerate" to Easings.EmphasizedDecelerate,
        )
        val je = leaves("motion.easing")
        assertEquals(je.keys, easings.keys)
        je.forEach { (k, v) ->
            val p = Regex("""cubic-bezier\(([^)]*)\)""").find(v as String)!!
                .groupValues[1].split(',').map { it.trim().toFloat() }
            val expected = androidx.compose.animation.core.CubicBezierEasing(p[0], p[1], p[2], p[3])
            // CubicBezierEasing has no equals: compare the curve at samples.
            for (i in 1..9) {
                val t = i / 10f
                assertTrue("$k at $t", abs(expected.transform(t) - easings.getValue(k).transform(t)) < 1e-4f)
            }
        }

        assertEquals(number(leaves("motion")["press-scale"]), PRESS_SCALE.toDouble(), 1e-6)
    }
}
