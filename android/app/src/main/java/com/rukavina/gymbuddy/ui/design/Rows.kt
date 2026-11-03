package com.rukavina.gymbuddy.ui.design

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.FullShape
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

/** Raw min heights from "01 System" (DESIGN.md §4). */
private val ListRowHeight = 68.dp
private val InfoRowHeight = 52.dp
private val GlyphTile = 44.dp
private val PrTagHeight = 20.dp

/**
 * Exercises, settings. Optional glyph tile on the left, chevron on the
 * right when tappable. Card radius 16 (D8), min 68.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes glyph: Int? = null,
    onClick: (() -> Unit)? = null,
) {
    val d = GymBuddyTheme.dimens
    RowCard(onClick, modifier, ListRowHeight) {
        if (glyph != null) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Box(Modifier.size(GlyphTile), contentAlignment = Alignment.Center) {
                    Icon(painterResource(glyph), null, Modifier.size(d.icon))
                }
            }
        }
        TitleBlock(title, subtitle, Modifier.weight(1f))
        if (onClick != null) Chevron()
    }
}

/** "Up next", "Edit note": muted label, value, chevron. Min 52. */
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    RowCard(onClick, modifier, InfoRowHeight) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onClick != null) Chevron()
    }
}

/** Home template row; the play button starts it, the row opens it. */
@Composable
fun TemplateRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RowCard(onClick, modifier, GymBuddyTheme.dimens.row, endPadding = GymBuddyTheme.dimens.hairline) {
        TitleBlock(title, subtitle, Modifier.weight(1f))
        AppIconButton(AppIcons.Ui.Play, stringResource(R.string.cd_start), onStart)
    }
}

/** Past session: title (+ PR tag), one-line summary, date, chevron. */
@Composable
fun SessionCard(
    title: String,
    subtitle: String,
    date: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPr: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    RowCard(onClick, modifier, d.row, verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.hairline)) {
            Row(horizontalArrangement = Arrangement.spacedBy(d.stackTight), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (isPr) PrTag()
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
        Text(
            date,
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        Chevron(Modifier.align(Alignment.CenterVertically))
    }
}

/** PR tag: text on a high pill, never colour alone. */
@Composable
fun PrTag(modifier: Modifier = Modifier) {
    Surface(shape = FullShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = modifier) {
        Box(
            Modifier.heightIn(min = PrTagHeight).padding(horizontal = GymBuddyTheme.dimens.stackTight),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.tag_pr), style = GymBuddyTheme.type.tag)
        }
    }
}

@Composable
private fun TitleBlock(title: String, subtitle: String?, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(GymBuddyTheme.dimens.hairline)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Chevron(modifier: Modifier = Modifier) {
    Icon(
        painterResource(AppIcons.Ui.ChevronRight),
        null,
        modifier.size(GymBuddyTheme.dimens.iconSmall),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Surface card (radius 16, D8) shared by every row; ripple, no press shrink. */
@Composable
private fun RowCard(
    onClick: (() -> Unit)?,
    modifier: Modifier,
    minHeight: Dp,
    endPadding: Dp = GymBuddyTheme.dimens.group,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit,
) {
    val d = GymBuddyTheme.dimens
    val shape = MaterialTheme.shapes.medium
    val color = MaterialTheme.colorScheme.surfaceContainer
    val body: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .padding(PaddingValues(start = d.group, end = endPadding, top = d.stackTight, bottom = d.stackTight)),
            horizontalArrangement = Arrangement.spacedBy(d.stack),
            verticalAlignment = verticalAlignment,
            content = content,
        )
    }
    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            shape = shape,
            color = color,
            interactionSource = interaction,
            modifier = modifier.focusRing(interaction, shape),
            content = body,
        )
    } else {
        Surface(shape = shape, color = color, modifier = modifier, content = body)
    }
}
