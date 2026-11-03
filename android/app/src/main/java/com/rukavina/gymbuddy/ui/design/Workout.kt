package com.rukavina.gymbuddy.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.FullShape
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.theme.Haptic
import com.rukavina.gymbuddy.ui.theme.rememberHaptics

/** Raw sizes from "01 System" (DESIGN.md §4). */
private val SetMark = 28.dp
private val RestIcon = 16.dp
private val StepperWidth = 64.dp

enum class SetState { Done, Upcoming }

/**
 * One set in the workout list. Done: surface fill, lime check. Upcoming:
 * outlined, muted value. Min 48 (D13), radius 12. Swipe comes in 7.6.
 */
@Composable
fun SetRow(
    setLabel: String,
    value: String,
    state: SetState,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val shape = MaterialTheme.shapes.small
    val done = state == SetState.Done
    val stateText = stringResource(if (done) R.string.state_set_done else R.string.state_set_upcoming)
    Surface(
        shape = shape,
        color = if (done) cs.surfaceContainer else Color.Transparent,
        border = if (done) null else BorderStroke(d.strokeDivider, cs.outline),
        modifier = modifier.semantics(mergeDescendants = true) { stateDescription = stateText },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = d.rowSet)
                .padding(horizontal = d.stack, vertical = d.stackTight),
            horizontalArrangement = Arrangement.spacedBy(d.group),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val markModifier = Modifier.size(SetMark)
            if (done) {
                Surface(shape = FullShape, color = GymBuddyTheme.colors.limeTint, modifier = markModifier) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(AppIcons.Ui.Check), null, Modifier.size(d.iconSmall), tint = cs.primary)
                    }
                }
            } else {
                Box(markModifier.border(d.strokeOutline, cs.outline, FullShape))
            }
            Text(setLabel, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                color = if (done) cs.onSurface else cs.onSurfaceVariant,
            )
        }
    }
}

/**
 * Rest between sets: always shown, one quiet muted line (DESIGN.md §1).
 * Figma draws the value in text colour; DESIGN.md wins (owner, 02b).
 */
@Composable
fun RestLine(value: String, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val d = GymBuddyTheme.dimens
    Row(
        modifier.heightIn(min = d.target).semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(d.stackTight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(AppIcons.Ui.Timer), null, Modifier.size(RestIcon), tint = muted)
        Text(value, style = MaterialTheme.typography.bodySmall, color = muted)
    }
}

enum class StepperSign { Minus, Plus }

/**
 * − / + key, 64×56, radius 16. One tick haptic per press; hold-to-repeat
 * comes with 7.6. [contentDescription] defaults to "Decrease"/"Increase".
 */
@Composable
fun StepperKey(
    sign: StepperSign,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val shape = MaterialTheme.shapes.medium
    val haptics = rememberHaptics()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val icon = if (sign == StepperSign.Minus) AppIcons.Ui.Remove else AppIcons.Ui.Add
    val cd = contentDescription
        ?: stringResource(if (sign == StepperSign.Minus) R.string.cd_decrease else R.string.cd_increase)
    Surface(
        onClick = { haptics(Haptic.StepperTick); onClick() },
        enabled = enabled,
        shape = shape,
        color = if (pressed) cs.surfaceContainerHigh else cs.surfaceContainerHighest,
        contentColor = if (enabled) cs.onSurface else GymBuddyTheme.colors.disabled,
        interactionSource = interaction,
        modifier = modifier
            .pressScale(interaction)
            .focusRing(interaction, shape)
            .semantics { role = Role.Button },
    ) {
        Box(Modifier.size(StepperWidth, d.key), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), cd, Modifier.size(d.icon))
        }
    }
}

/**
 * Weight or reps on the set card. Tapping the number opens the keypad
 * ([onClick]); [editing] draws the lime line and shows [step] ("± 2.5 kg").
 */
@Composable
fun ValuePanel(
    number: String,
    unit: String,
    editing: Boolean,
    onClick: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    step: String? = null,
    minusEnabled: Boolean = true,
    plusEnabled: Boolean = true,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val shape = MaterialTheme.shapes.medium
    Surface(
        shape = shape,
        color = cs.surfaceContainerHigh,
        border = if (editing) BorderStroke(d.strokeEmphasis, cs.primary) else null,
        modifier = modifier,
    ) {
        Column(
            Modifier.padding(horizontal = d.stack, vertical = d.group),
            verticalArrangement = Arrangement.spacedBy(d.stack),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = d.target)
                    .selectable(selected = editing, onClick = onClick, role = Role.Button),
                horizontalArrangement = Arrangement.spacedBy(d.stackTight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(number, style = MaterialTheme.typography.displayLarge, modifier = Modifier.alignByBaseline())
                Text(
                    unit,
                    style = MaterialTheme.typography.titleSmall,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            if (editing && step != null) {
                Text(step, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StepperKey(StepperSign.Minus, onMinus, enabled = minusEnabled)
                StepperKey(StepperSign.Plus, onPlus, enabled = plusEnabled)
            }
        }
    }
}

/**
 * The current set: title and two [ValuePanel]s, cobalt ring ("you are
 * here"), radius 20. Give each panel `Modifier.weight(1f)`.
 */
@Composable
fun SetCard(
    title: String,
    modifier: Modifier = Modifier,
    panels: @Composable RowScope.() -> Unit,
) {
    val d = GymBuddyTheme.dimens
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(d.strokeEmphasis, GymBuddyTheme.colors.cobalt),
        modifier = modifier,
    ) {
        Column(Modifier.padding(d.insetCard), verticalArrangement = Arrangement.spacedBy(d.stack)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(d.stackTight), content = panels)
        }
    }
}
