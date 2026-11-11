package com.rukavina.gymbuddy.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.FullShape
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

/**
 * The one text field (D10): label above, raised 56 field, helper
 * below. Focus = 2dp cobalt line, error = 2dp danger line plus red
 * label and helper (never colour alone: the helper says what's wrong).
 * Caret is cobalt; lime only marks things you press.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helper: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    /** Field fill; one step above whatever it sits on. Dialogs pass [AppTextFieldDefaults.onDialog] (F10). */
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    val cs = MaterialTheme.colorScheme
    val c = GymBuddyTheme.colors
    val d = GymBuddyTheme.dimens
    val type = MaterialTheme.typography
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val accent = if (isError) cs.error else cs.onSurfaceVariant
    val line = when {
        isError -> cs.error
        focused -> c.cobalt
        else -> null
    }
    // Label and helper live in the decoration box, so they merge into the
    // field's semantics node and TalkBack reads "Email, edit box".
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        textStyle = type.bodyLarge.copy(color = if (enabled) cs.onSurface else c.disabled),
        cursorBrush = SolidColor(c.cobalt),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (isError && helper != null) error(helper) },
        decorationBox = { inner ->
            Column(verticalArrangement = Arrangement.spacedBy(d.stackTight)) {
                Text(label, style = type.labelMedium, color = accent)
                Row(
                    modifier = Modifier
                        .heightIn(min = d.control)
                        .background(container, MaterialTheme.shapes.small)
                        .then(
                            if (line != null) Modifier.border(d.strokeEmphasis, line, MaterialTheme.shapes.small)
                            else Modifier,
                        )
                        .padding(start = d.group, end = if (trailing != null) d.hairline else d.group),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f).padding(vertical = d.stack)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = type.bodyLarge, color = cs.onSurfaceVariant)
                        }
                        inner()
                    }
                    trailing?.invoke()
                }
                if (helper != null) Text(helper, style = type.bodySmall, color = accent)
            }
        },
    )
}

/** Docked search pill with a clear × once filled. */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val cs = MaterialTheme.colorScheme
    val c = GymBuddyTheme.colors
    val d = GymBuddyTheme.dimens
    val type = MaterialTheme.typography
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = type.bodyLarge.copy(color = cs.onSurface),
        cursorBrush = SolidColor(c.cobalt),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = keyboardActions,
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .heightIn(min = 52.dp)
                    .background(cs.surfaceContainerHigh, FullShape)
                    .then(if (focused) Modifier.border(d.strokeEmphasis, c.cobalt, FullShape) else Modifier)
                    .padding(start = d.group, end = if (value.isEmpty()) d.group else d.hairline),
                horizontalArrangement = Arrangement.spacedBy(d.stack),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(AppIcons.Ui.Search), null, Modifier.size(d.icon), tint = cs.onSurfaceVariant)
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = type.bodyLarge, color = cs.onSurfaceVariant)
                    inner()
                }
                if (value.isNotEmpty()) {
                    AppIconButton(
                        icon = AppIcons.Ui.Close,
                        contentDescription = stringResource(R.string.cd_clear_search),
                        onClick = { onValueChange("") },
                    )
                }
            }
        },
    )
}

/** Fills for [AppTextField] on raised surfaces. */
object AppTextFieldDefaults {
    /** Dialogs are surfaceContainerHigh, the page field's own fill, so fields there go one step up. */
    val onDialog: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerHighest
}
