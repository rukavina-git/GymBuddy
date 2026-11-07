package com.rukavina.gymbuddy.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.FullShape
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

enum class BannerTone { Danger, Info }

private val BannerIcon = 22.dp
private val EmptyGlyphTile = 80.dp
private val EmptyGlyph = 40.dp
private val SnackbarHeight = 56.dp
private val SnackbarShadow = 6.dp

/**
 * "Couldn't save", "Needs internet". Danger sits on the danger
 * container; Info on surface. The action is lime text (you can press it).
 */
@Composable
fun Banner(
    tone: BannerTone,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = if (tone == BannerTone.Danger) AppIcons.Ui.Alert else AppIcons.Ui.CloudOff,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (tone == BannerTone.Danger) cs.errorContainer else cs.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(d.insetCard), horizontalArrangement = Arrangement.spacedBy(d.stack)) {
            Icon(
                painterResource(icon),
                null,
                Modifier.size(BannerIcon),
                tint = if (tone == BannerTone.Danger) cs.error else cs.onSurfaceVariant,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.hairline)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodySmall, color = GymBuddyTheme.colors.textSoft)
                if (actionLabel != null && onAction != null) {
                    LinkAction(actionLabel, onAction)
                }
            }
        }
    }
}

/** Empty lists: glyph in a round tile, title, one line, optional lime action. */
@Composable
fun EmptyState(
    @DrawableRes glyph: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(d.stack),
    ) {
        Surface(shape = FullShape, color = cs.surfaceContainer) {
            Box(Modifier.size(EmptyGlyphTile), contentAlignment = Alignment.Center) {
                Icon(painterResource(glyph), null, Modifier.size(EmptyGlyph), tint = cs.onSurfaceVariant)
            }
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            PrimaryButton(actionLabel, onAction)
        }
    }
}

/**
 * For `SnackbarHost { AppSnackbar(it) }`: high surface, lime action, min 56 (D12).
 * F9: an outline and a shadow lift it off dark pages; full redesign is F21d.
 */
@Composable
fun AppSnackbar(data: SnackbarData, modifier: Modifier = Modifier) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.small,
        color = cs.surfaceContainerHighest,
        contentColor = cs.onSurface,
        border = BorderStroke(d.strokeDivider, cs.outline),
        shadowElevation = SnackbarShadow,
        modifier = modifier.padding(horizontal = d.insetScreen).fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = SnackbarHeight)
                .padding(start = d.group, end = d.stackTight),
            horizontalArrangement = Arrangement.spacedBy(d.stack),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(data.visuals.message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            data.visuals.actionLabel?.let { LinkAction(it, data::performAction, MaterialTheme.typography.labelLarge, d.stackTight) }
            if (data.visuals.withDismissAction) {
                AppIconButton(AppIcons.Ui.Close, stringResource(R.string.cd_dismiss), data::dismiss)
            }
        }
    }
}

@Composable
private fun LinkAction(
    label: String,
    onClick: () -> Unit,
    style: TextStyle = MaterialTheme.typography.titleSmall,
    horizontalPadding: Dp = 0.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.extraSmall
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .focusRing(interaction, shape)
            .clip(shape)
            .clickable(interaction, ripple(), role = Role.Button, onClick = onClick)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, style = style, color = MaterialTheme.colorScheme.primary)
    }
}
