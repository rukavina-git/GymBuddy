package com.rukavina.gymbuddy.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.theme.Haptic
import com.rukavina.gymbuddy.ui.theme.rememberHaptics
import java.text.DecimalFormatSymbols

/** Raw sizes from "01 System" (DESIGN.md §4). */
private val DisplayHeight = 76.dp
private val DisplayPadding = 20.dp
private val CaretHeight = 40.dp

/** What a keypad press means. Parsing and limits live in 7.6. */
sealed interface KeypadInput {
    data class Digit(val n: Int) : KeypadInput {
        init { require(n in 0..9) }
    }
    data object Decimal : KeypadInput
    data object Backspace : KeypadInput
    data object Next : KeypadInput
    data object Cancel : KeypadInput
}

/** Grid order: 1–9, then decimal, 0, backspace. */
internal val KeypadGrid: List<KeypadInput> =
    (1..9).map { KeypadInput.Digit(it) } + listOf(KeypadInput.Decimal, KeypadInput.Digit(0), KeypadInput.Backspace)

/**
 * One key, 56 high, radius 12, press shrink, keyboard-tap haptic.
 * Digit and decimal show text; backspace shows the icon.
 */
@Composable
fun KeypadKey(
    input: KeypadInput,
    onInput: (KeypadInput) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    require(input !is KeypadInput.Next && input !is KeypadInput.Cancel) { "Next/Cancel are buttons" }
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val shape = MaterialTheme.shapes.small
    val haptics = rememberHaptics()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Surface(
        onClick = { haptics(Haptic.KeypadKey); onInput(input) },
        enabled = enabled,
        shape = shape,
        color = if (pressed) cs.surfaceContainerHighest else cs.surfaceContainerHigh,
        contentColor = if (enabled) cs.onSurface else GymBuddyTheme.colors.disabled,
        interactionSource = interaction,
        modifier = modifier
            .pressScale(interaction)
            .focusRing(interaction, shape)
            .semantics { role = Role.Button },
    ) {
        Box(Modifier.heightIn(min = d.key), contentAlignment = Alignment.Center) {
            when (input) {
                is KeypadInput.Digit -> Text(input.n.toString(), style = MaterialTheme.typography.headlineSmall)
                KeypadInput.Decimal -> Text(
                    DecimalFormatSymbols.getInstance().decimalSeparator.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                )
                else -> Icon(
                    painterResource(AppIcons.Ui.Backspace),
                    stringResource(R.string.cd_backspace),
                    Modifier.size(d.icon),
                )
            }
        }
    }
}

/**
 * Keypad sheet content: label, value with lime caret, unit, 4×3 keys,
 * Cancel + Next (56, D11). Stateless: emits [KeypadInput] only. Put it
 * in a bottom sheet; the sheet gives the handle.
 */
@Composable
fun Keypad(
    label: String,
    value: String,
    unit: String,
    onInput: (KeypadInput) -> Unit,
    modifier: Modifier = Modifier,
    decimalEnabled: Boolean = true,
    nextEnabled: Boolean = true,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    Column(modifier, verticalArrangement = Arrangement.spacedBy(d.stackTight)) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant)
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = cs.surfaceContainerHigh,
            border = BorderStroke(d.strokeEmphasis, cs.primary),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.heightIn(min = DisplayHeight).padding(horizontal = DisplayPadding),
                horizontalArrangement = Arrangement.spacedBy(d.stack),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(value, style = MaterialTheme.typography.displayMedium)
                Box(Modifier.size(d.strokeEmphasis, CaretHeight).background(cs.primary))
                Text(unit, style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant)
            }
        }
        Column(
            Modifier.padding(top = d.stackTight),
            verticalArrangement = Arrangement.spacedBy(d.stackTight),
        ) {
            KeypadGrid.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(d.stackTight)) {
                    row.forEach { key ->
                        KeypadKey(
                            key,
                            onInput,
                            Modifier.weight(1f),
                            enabled = key != KeypadInput.Decimal || decimalEnabled,
                        )
                    }
                }
            }
        }
        Row(
            Modifier.padding(top = d.stack),
            horizontalArrangement = Arrangement.spacedBy(d.stackTight),
        ) {
            OutlineButton(
                stringResource(R.string.keypad_cancel),
                { onInput(KeypadInput.Cancel) },
                Modifier.weight(1f),
            )
            PrimaryButton(
                stringResource(R.string.keypad_next),
                { onInput(KeypadInput.Next) },
                Modifier.weight(1f),
                enabled = nextEnabled,
            )
        }
    }
}
