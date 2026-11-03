package com.rukavina.gymbuddy.ui.design

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.FullShape
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

/** Raw sizes from "01 System" (DESIGN.md §4). */
private val OpenPillWidth = 60.dp
private val OpenPillHeight = 44.dp
private val ProgressLine = 3.dp
private val StatTilePadding = 14.dp
private val StatTileGap = 2.dp
private val DayDot = 36.dp

/**
 * Running workout, on every tab. Title, subtitle, lime open pill and a
 * lime progress line along the top edge ([progress] 0..1). The whole
 * bar opens the workout.
 */
@Composable
fun LiveBar(
    title: String,
    subtitle: String,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val shape = MaterialTheme.shapes.medium
    val fraction = progress.coerceIn(0f, 1f)
    val lime = cs.primary
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = shape,
        color = cs.surfaceContainerHigh,
        interactionSource = interaction,
        modifier = modifier
            .focusRing(interaction, shape)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = d.row)
                .drawWithContent {
                    drawContent()
                    val y = ProgressLine.toPx() / 2
                    if (fraction > 0f) {
                        drawLine(
                            lime, Offset(0f, y), Offset(size.width * fraction, y),
                            strokeWidth = ProgressLine.toPx(), cap = StrokeCap.Round,
                        )
                    }
                }
                .padding(horizontal = d.group, vertical = d.stackTight),
            horizontalArrangement = Arrangement.spacedBy(d.stack),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.hairline)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1)
            }
            Surface(shape = FullShape, color = lime, contentColor = cs.onPrimary) {
                Box(Modifier.size(OpenPillWidth, OpenPillHeight), contentAlignment = Alignment.Center) {
                    Icon(painterResource(AppIcons.Ui.ChevronRight), null, Modifier.size(d.icon))
                }
            }
        }
    }
}

/** Summary number: value (`stat-value`) over a muted label, min 72 (D7). */
@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val d = GymBuddyTheme.dimens
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        Column(
            Modifier.heightIn(min = d.row).padding(horizontal = StatTilePadding, vertical = d.stackTight),
            verticalArrangement = Arrangement.spacedBy(StatTileGap, Alignment.CenterVertically),
        ) {
            Text(value, style = GymBuddyTheme.type.statValue)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

enum class WeekDayState { Rest, Done, Today, TodayDone }

/** One day on the week strip. [day] is the short label ("M"), [dayName] is read aloud. */
data class WeekDayUi(val day: String, val dayName: String, val state: WeekDayState)

@Composable
internal fun weekDayDescription(dayName: String, state: WeekDayState): String = stringResource(
    when (state) {
        WeekDayState.Rest -> R.string.week_day_rest
        WeekDayState.Done -> R.string.week_day_done
        WeekDayState.Today -> R.string.week_day_today
        WeekDayState.TodayDone -> R.string.week_day_today_done
    },
    dayName,
)

/** Day letter over a 36 dot: raised (rest), high + lime check (done), cobalt ring (today). */
@Composable
fun WeekDay(day: WeekDayUi, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val done = day.state == WeekDayState.Done || day.state == WeekDayState.TodayDone
    val today = day.state == WeekDayState.Today || day.state == WeekDayState.TodayDone
    val description = weekDayDescription(day.dayName, day.state)
    Column(
        modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(d.stackTight),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(day.day, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
        val fill = when {
            done -> cs.surfaceContainerHighest
            today -> null
            else -> cs.surfaceContainerHigh
        }
        Surface(
            shape = FullShape,
            color = fill ?: Color.Transparent,
            modifier = Modifier
                .size(DayDot)
                .then(if (today) Modifier.border(d.strokeEmphasis, GymBuddyTheme.colors.cobalt, FullShape) else Modifier),
        ) {
            if (done) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(AppIcons.Ui.Check), null, Modifier.size(d.iconSmall), tint = cs.primary)
                }
            }
        }
    }
}

/** Home week card: seven [WeekDay]s and a summary line (opens Progress when [onClick] is set). */
@Composable
fun WeekStrip(
    days: List<WeekDayUi>,
    summary: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    require(days.size == 7) { "A week has 7 days, got ${days.size}" }
    val d = GymBuddyTheme.dimens
    val shape = MaterialTheme.shapes.large
    val color = MaterialTheme.colorScheme.surfaceContainer
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(d.insetCard), verticalArrangement = Arrangement.spacedBy(d.stack)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = d.stackTight),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                days.forEach { WeekDay(it) }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = d.group),
                horizontalArrangement = Arrangement.spacedBy(d.stack),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(summary, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (onClick != null) {
                    Icon(
                        painterResource(AppIcons.Ui.ChevronRight),
                        null,
                        Modifier.size(d.icon),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick, shape = shape, color = color, interactionSource = interaction,
            modifier = modifier.focusRing(interaction, shape), content = body,
        )
    } else {
        Surface(shape = shape, color = color, modifier = modifier, content = body)
    }
}
