package com.rukavina.gymbuddy.ui.design

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import com.rukavina.gymbuddy.R
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.ui.theme.FullShape
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

/**
 * Button sizes from "01 System" (DESIGN.md §4, raw sizes as drawn).
 * Heights are minimums: buttons grow with font scale. Tap area stays
 * 48dp even for [Small] (Surface applies the minimum touch target).
 */
enum class ButtonSize(internal val height: Dp, internal val icon: Dp, internal val wide: Boolean) {
    Medium(56.dp, 24.dp, true),
    /** 48: form actions under 56dp fields (sign-in, create account; Figma R18). */
    Form(48.dp, 20.dp, true),
    /** 40: top bar Save, small secondary/outline. */
    Small(40.dp, 20.dp, false),
}

/** Lime: "you can press this". Start, log, done, save. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.Medium,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    loading: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val c = GymBuddyTheme.colors
    PillButton(
        text = text, onClick = onClick, modifier = modifier, size = size, enabled = enabled, icon = icon,
        container = if (enabled || loading) cs.primary else cs.surfaceContainerHigh,
        pressedContainer = c.limePressed,
        content = if (enabled || loading) cs.onPrimary else c.disabled,
        loading = loading,
    )
}

/** Raised fill: All sessions, Swap. Sizes 56 / 40. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.Medium,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    val cs = MaterialTheme.colorScheme
    PillButton(
        text = text, onClick = onClick, modifier = modifier, size = size, enabled = enabled, icon = icon,
        container = cs.surfaceContainerHigh,
        content = if (enabled) cs.onSurface else GymBuddyTheme.colors.disabled,
    )
}

/** 1.5dp outline: I have an account, Cancel, Keep going. Sizes 56 / 40. */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.Medium,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    loading: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val disabled = GymBuddyTheme.colors.disabled
    PillButton(
        text = text, onClick = onClick, modifier = modifier, size = size, enabled = enabled, icon = icon,
        container = Color.Transparent,
        content = if (enabled || loading) cs.onSurface else disabled,
        border = BorderStroke(GymBuddyTheme.dimens.strokeOutline, if (enabled || loading) cs.outline else disabled),
        loading = loading,
    )
}

/** Dialog actions. 40 high, no fill. */
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    PillButton(
        text = text, onClick = onClick, modifier = modifier, size = ButtonSize.Small, enabled = enabled, icon = icon,
        container = Color.Transparent,
        content = if (enabled) MaterialTheme.colorScheme.onSurface else GymBuddyTheme.colors.disabled,
        horizontalPadding = GymBuddyTheme.dimens.stack,
    )
}

/** Lime text: the action you're meant to press in a dialog or banner (Figma 114:20520). */
@Composable
fun AppLinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    PillButton(
        text = text, onClick = onClick, modifier = modifier, size = ButtonSize.Small, enabled = enabled, icon = null,
        container = Color.Transparent,
        content = if (enabled) MaterialTheme.colorScheme.primary else GymBuddyTheme.colors.disabled,
        horizontalPadding = GymBuddyTheme.dimens.stack,
    )
}

/** Discard, Delete. Never the default action, never lime. */
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    PillButton(
        text = text, onClick = onClick, modifier = modifier, size = ButtonSize.Small, enabled = enabled, icon = icon,
        container = Color.Transparent,
        content = if (enabled) MaterialTheme.colorScheme.error else GymBuddyTheme.colors.disabled,
        horizontalPadding = GymBuddyTheme.dimens.stack,
    )
}

/** Filled red, full width: only the final step of an action you can't undo (Delete account screen). */
@Composable
fun DangerFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    PillButton(
        text = text, onClick = onClick, modifier = modifier, size = ButtonSize.Medium, enabled = enabled, icon = null,
        container = if (enabled || loading) cs.error else cs.surfaceContainerHigh,
        content = if (enabled || loading) cs.onError else GymBuddyTheme.colors.disabled,
        loading = loading,
    )
}

enum class IconButtonStyle { Lime, Raised, High, Plain }

/** 48dp round icon button. Lime only for a primary action (Play). */
@Composable
fun AppIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IconButtonStyle = IconButtonStyle.Plain,
    enabled: Boolean = true,
) {
    val cs = MaterialTheme.colorScheme
    val c = GymBuddyTheme.colors
    val container = when {
        style == IconButtonStyle.Plain -> Color.Transparent
        !enabled -> cs.surfaceContainerHigh
        style == IconButtonStyle.Lime -> cs.primary
        style == IconButtonStyle.Raised -> cs.surfaceContainerHigh
        else -> cs.surfaceContainerHighest
    }
    val content = when {
        !enabled -> c.disabled
        style == IconButtonStyle.Lime -> cs.onPrimary
        else -> cs.onSurface
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = FullShape,
        color = if (pressed && style == IconButtonStyle.Lime) c.limePressed else container,
        contentColor = content,
        interactionSource = interaction,
        modifier = modifier
            .pressScale(interaction)
            .focusRing(interaction, FullShape)
            .semantics { role = Role.Button },
    ) {
        Box(Modifier.size(GymBuddyTheme.dimens.target), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription, Modifier.size(GymBuddyTheme.dimens.icon))
        }
    }
}

@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    size: ButtonSize,
    enabled: Boolean,
    @DrawableRes icon: Int?,
    container: Color,
    content: Color,
    pressedContainer: Color = container,
    border: BorderStroke? = null,
    horizontalPadding: Dp? = null,
    loading: Boolean = false,
) {
    val d = GymBuddyTheme.dimens
    val working = stringResource(R.string.state_working)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Surface(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = FullShape,
        color = if (pressed) pressedContainer else container,
        contentColor = content,
        border = border,
        interactionSource = interaction,
        modifier = modifier
            .pressScale(interaction)
            .focusRing(interaction, FullShape)
            .semantics {
                role = Role.Button
                if (loading) stateDescription = working
            },
    ) {
        val h = horizontalPadding ?: if (size.wide) d.section else d.group
        Row(
            modifier = Modifier
                .heightIn(min = size.height)
                .padding(PaddingValues(horizontal = h, vertical = d.stackTight)),
            horizontalArrangement = Arrangement.spacedBy(d.stackTight, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                // The label stays for TalkBack; the spinner replaces it on screen.
                CircularProgressIndicator(
                    color = content,
                    strokeWidth = d.strokeEmphasis,
                    modifier = Modifier.size(size.icon).semantics { contentDescription = text },
                )
            } else {
                if (icon != null) Icon(painterResource(icon), null, Modifier.size(size.icon))
                Text(text, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
