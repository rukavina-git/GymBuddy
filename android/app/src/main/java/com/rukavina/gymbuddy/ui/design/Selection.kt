package com.rukavina.gymbuddy.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.FullShape
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.theme.Haptic
import com.rukavina.gymbuddy.ui.theme.rememberHaptics

private val ChipIcon = 18.dp

/**
 * Filter / choice chip. Selected = cobalt tint + cobalt line + check
 * (state is never colour alone). Drawn 36 high, 48dp tap area.
 * [dropdown] adds the chevron for chips that open a picker.
 */
@Composable
fun AppChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dropdown: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val c = GymBuddyTheme.colors
    val d = GymBuddyTheme.dimens
    val interaction = remember { MutableInteractionSource() }
    val content = when {
        !enabled -> c.disabled
        selected -> cs.onSurface
        else -> c.textSoft
    }
    Surface(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        shape = FullShape,
        color = if (selected && enabled) c.cobaltTint else cs.surfaceContainerHigh,
        contentColor = content,
        border = if (selected && enabled) BorderStroke(d.strokeDivider, c.cobalt) else null,
        interactionSource = interaction,
        // Selectable Surface skips the 48dp minimum: add it (drawn 36).
        modifier = modifier.minimumInteractiveComponentSize().focusRing(interaction, FullShape),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = d.chip)
                .padding(start = if (selected) 10.dp else d.group, end = d.group),
            horizontalArrangement = Arrangement.spacedBy(d.stackTight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) Icon(painterResource(AppIcons.Ui.Check), null, Modifier.size(ChipIcon))
            Text(label, style = MaterialTheme.typography.labelMedium)
            if (dropdown) Icon(painterResource(AppIcons.Ui.ChevronDown), null, Modifier.size(ChipIcon))
        }
    }
}

/** On = cobalt ("you are here"), never lime. Toggle haptics per motion.md. */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val cs = MaterialTheme.colorScheme
    val c = GymBuddyTheme.colors
    val haptics = rememberHaptics()
    Switch(
        checked = checked,
        onCheckedChange = {
            haptics(if (it) Haptic.SwitchOn else Haptic.SwitchOff)
            onCheckedChange(it)
        },
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = cs.onSurface,
            checkedTrackColor = c.cobalt,
            checkedBorderColor = c.cobalt,
            uncheckedThumbColor = cs.outline,
            uncheckedTrackColor = cs.surfaceContainerHigh,
            uncheckedBorderColor = cs.outline,
            disabledCheckedThumbColor = c.disabled,
            disabledCheckedTrackColor = cs.surfaceContainerHigh,
            disabledCheckedBorderColor = c.disabled,
            disabledUncheckedThumbColor = c.disabled,
            disabledUncheckedTrackColor = cs.surfaceContainerHigh,
            disabledUncheckedBorderColor = c.disabled,
        ),
    )
}

/**
 * Two-or-more option pill (List / Calendar). Selected segment is
 * cobalt with a check and bolder text; 48 high.
 */
@Composable
fun SegmentedButtons(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val c = GymBuddyTheme.colors
    val d = GymBuddyTheme.dimens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = d.target)
            .border(d.strokeDivider, cs.outline, FullShape)
            .padding(d.strokeDivider)
            .selectableGroup(),
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            val interaction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .focusRing(interaction, FullShape)
                    .clip(FullShape)
                    .selectable(
                        selected = selected,
                        interactionSource = interaction,
                        indication = ripple(),
                        role = Role.RadioButton,
                        onClick = { onSelect(i) },
                    )
                    .then(if (selected) Modifier.background(c.cobalt, FullShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(d.stackTight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // text/on-lime on cobalt (Figma); 5.5:1.
                    val color = if (selected) cs.onPrimary else cs.onSurface
                    if (selected) Icon(painterResource(AppIcons.Ui.Check), null, tint = color)
                    Text(
                        label,
                        color = color,
                        style = if (selected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
