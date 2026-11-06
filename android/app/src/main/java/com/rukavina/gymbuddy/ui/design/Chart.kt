package com.rukavina.gymbuddy.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.domain.progress.ProgressStats
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import kotlin.math.roundToInt

/** Raw sizes from Figma R7-1 / R7-5 (DESIGN.md §4): sparkline 64 high, chart plot 164, y label column 40. */
private val SparkHeight = 64.dp
private val PlotHeight = 164.dp
private val YLabelWidth = 40.dp
private val DotRadius = 3.5.dp
private val LastDotRadius = 5.dp
private val ActiveDotRadius = 8.dp
private val GuideStroke = 1.5.dp
private val ActiveRing = 3.dp

/**
 * Sparkline on the Progress card (R7-1): line, filled last dot, no axes.
 * Decorative: the card's sentence carries the meaning.
 */
@Composable
fun Sparkline(values: List<Float>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.onSurface
    val d = GymBuddyTheme.dimens
    Canvas(modifier.fillMaxWidth().height(SparkHeight).clearAndSetSemantics {}) {
        if (values.size < 2) return@Canvas
        val pad = LastDotRadius.toPx()
        val lo = values.min()
        val hi = values.max()
        val pts = layout(values, lo, hi, pad, size.width - pad, pad, size.height - pad)
        drawLine(pts, line, d.strokeChart.toPx())
        drawCircle(line, LastDotRadius.toPx(), pts.last())
    }
}

/**
 * Lift chart (R7-5, drag R7-6): three gridlines with y labels, the line,
 * a dot per session (last one filled), three dates under it. Press and
 * drag snaps to the nearest session and shows [readout] above; nothing
 * opens (motion.md #26: the line doesn't animate). One TalkBack stop
 * reading [summary].
 */
@Composable
fun LiftChart(
    values: List<Float>,
    yTicks: List<Float>,
    yLabel: (Float) -> String,
    xLabels: List<String>,
    readout: (Int) -> Pair<String, String>,
    summary: String,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val card = cs.surfaceContainer
    var active by remember { mutableStateOf<Int?>(null) }
    Column(modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = summary }) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val plotWidthPx = constraints.maxWidth - with(androidx.compose.ui.platform.LocalDensity.current) { (YLabelWidth + LastDotRadius * 2).toPx() }
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.width(YLabelWidth).height(PlotHeight)) {
                    yTicks.forEachIndexed { i, tick ->
                        val frac = 1f - i / (yTicks.size - 1).coerceAtLeast(1).toFloat()
                        Text(
                            yLabel(tick),
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.offset { IntOffset(0, (frac * (PlotHeight - 16.dp).toPx()).roundToInt()) },
                        )
                    }
                }
                Canvas(
                    Modifier
                        .weight(1f)
                        .height(PlotHeight)
                        .pointerInput(values.size) {
                            val inset = LastDotRadius.toPx()
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                fun pick(x: Float) {
                                    active = ProgressStats.nearestIndex(x - inset, size.width - 2 * inset, values.size)
                                }
                                pick(down.position.x)
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    pick(change.position.x)
                                    // Keep the drag here, not in the page scroll.
                                    if (change.position.x != change.previousPosition.x) change.consume()
                                }
                                active = null
                            }
                        },
                ) {
                    if (values.size < 2 || yTicks.size < 2) return@Canvas
                    val inset = LastDotRadius.toPx()
                    val lo = yTicks.first()
                    val hi = yTicks.last()
                    val top = 8.dp.toPx()
                    val bottom = size.height - 8.dp.toPx()
                    yTicks.forEachIndexed { i, _ ->
                        val y = bottom - (bottom - top) * i / (yTicks.size - 1)
                        drawLine(cs.surfaceContainerHighest, Offset(0f, y), Offset(size.width, y), d.strokeDivider.toPx())
                    }
                    val pts = layout(values, lo, hi, inset, size.width - inset, top, bottom)
                    active?.let { a ->
                        drawLine(cs.onSurfaceVariant, Offset(pts[a].x, 0f), Offset(pts[a].x, size.height), GuideStroke.toPx())
                    }
                    drawLine(pts, cs.onSurface, d.strokeChart.toPx())
                    pts.forEachIndexed { i, p ->
                        when {
                            i == active -> {
                                drawCircle(cs.onSurface, ActiveDotRadius.toPx(), p)
                                drawCircle(card, ActiveDotRadius.toPx(), p, style = Stroke(ActiveRing.toPx()))
                            }
                            i == pts.lastIndex -> drawCircle(cs.onSurface, LastDotRadius.toPx(), p)
                            else -> {
                                drawCircle(card, DotRadius.toPx(), p)
                                drawCircle(cs.onSurface, DotRadius.toPx(), p, style = Stroke(d.strokeEmphasis.toPx()))
                            }
                        }
                    }
                }
            }
            active?.let { a ->
                val (date, value) = readout(a)
                val step = if (values.size > 1) plotWidthPx / (values.size - 1) else 0f
                Readout(
                    date, value,
                    Modifier.offset {
                        val x = YLabelWidth.toPx() + LastDotRadius.toPx() + step * a - ReadoutWidth.toPx() / 2
                        IntOffset(x.coerceIn(0f, constraints.maxWidth - ReadoutWidth.toPx()).roundToInt(), 0)
                    },
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = YLabelWidth, top = d.hairline)) {
            xLabels.forEachIndexed { i, label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    textAlign = when (i) {
                        0 -> TextAlign.Start
                        xLabels.lastIndex -> TextAlign.End
                        else -> TextAlign.Center
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private val ReadoutWidth = 150.dp

/** "Mon 5 Oct" over "85 kg × 5" on a raised pill (R7-6). */
@Composable
private fun Readout(date: String, value: String, modifier: Modifier) {
    val d = GymBuddyTheme.dimens
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = modifier.width(ReadoutWidth)) {
        Column(Modifier.padding(horizontal = d.stack, vertical = d.hairline), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"))
        }
    }
}

/** Evenly spaced points; [lo]..[hi] maps to [bottom]..[top]. */
private fun layout(values: List<Float>, lo: Float, hi: Float, left: Float, right: Float, top: Float, bottom: Float): List<Offset> {
    val span = (hi - lo).takeIf { it > 0f } ?: 1f
    val step = if (values.size > 1) (right - left) / (values.size - 1) else 0f
    return values.mapIndexed { i, v ->
        val y = if (hi == lo) (top + bottom) / 2 else bottom - (v - lo) / span * (bottom - top)
        Offset(left + step * i, y)
    }
}

private fun DrawScope.drawLine(points: List<Offset>, color: Color, width: Float) {
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
