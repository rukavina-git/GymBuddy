package com.rukavina.gymbuddy.ui.design

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.ui.theme.FullShape
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.theme.SheetShape

private val DialogWidth = 356.dp
private val HandleWidth = 32.dp
private val HandleHeight = 4.dp
private val MenuWidth = 220.dp

/**
 * Discard / Delete / Sign out (D4: surface-raised, 356 wide). Actions
 * sit right: dismiss as a text button, confirm as text or danger.
 * M3 dialog motion (motion.md row 17).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    val d = GymBuddyTheme.dimens
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = DialogWidth),
        ) {
            Column(
                modifier = Modifier.padding(start = d.insetDialog, end = d.insetDialog, top = d.insetDialog, bottom = d.group),
                verticalArrangement = Arrangement.spacedBy(d.group),
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(d.stackTight, Alignment.End),
                ) {
                    AppTextButton(dismissLabel, onDismiss)
                    if (destructive) DangerButton(confirmLabel, onConfirm) else AppTextButton(confirmLabel, onConfirm)
                }
            }
        }
    }
}

enum class DialogActionTone { Lime, Plain, Danger }

data class DialogAction(val label: String, val onClick: () -> Unit, val tone: DialogActionTone = DialogActionTone.Plain)

/**
 * Account questions (Figma 114:20520): title, body and two or three
 * actions; the one to press is lime. [stacked] puts the actions in a
 * right-aligned column, for labels too long to sit side by side.
 * [onDismiss] null: a choice is needed, outside taps do nothing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppChoiceDialog(
    title: String,
    body: String,
    actions: List<DialogAction>,
    onDismiss: (() -> Unit)? = null,
    stacked: Boolean = false,
) {
    val d = GymBuddyTheme.dimens
    BasicAlertDialog(onDismissRequest = { onDismiss?.invoke() }) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = DialogWidth),
        ) {
            Column(
                modifier = Modifier.padding(start = d.insetDialog, end = d.insetDialog, top = d.insetDialog, bottom = d.group),
                verticalArrangement = Arrangement.spacedBy(d.group),
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val buttons = @Composable {
                    actions.forEach { action ->
                        when (action.tone) {
                            DialogActionTone.Lime -> AppLinkButton(action.label, action.onClick)
                            DialogActionTone.Plain -> AppTextButton(action.label, action.onClick)
                            DialogActionTone.Danger -> DangerButton(action.label, action.onClick)
                        }
                    }
                }
                if (stacked) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) { buttons() }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(d.stackTight, Alignment.End),
                    ) { buttons() }
                }
            }
        }
    }
}

/**
 * Change lift, Start workout. Surface fill (D3 amended), top corners
 * 28, own handle. Motion stays M3 default (DESIGN.md §7). [title] null:
 * the content brings its own label (keypad, R12-11).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    title: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        shape = SheetShape,
        containerColor = cs.surfaceContainer,
        contentColor = cs.onSurface,
        scrimColor = cs.scrim,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(Modifier.padding(top = d.stack, bottom = d.group)) {
                Box(Modifier.size(HandleWidth, HandleHeight).background(cs.outline, FullShape))
            }
        },
    ) {
        Column(
            modifier = Modifier.padding(start = d.insetDialog, end = d.insetDialog, bottom = d.insetDialog),
            verticalArrangement = Arrangement.spacedBy(d.stackTight),
        ) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            Column(
                modifier = Modifier.padding(top = if (title != null || subtitle != null) d.group else 0.dp),
                verticalArrangement = Arrangement.spacedBy(d.stackTight),
                content = content,
            )
        }
    }
}

/** ⋯ menu: raised, radius 16, 220 wide. Fill with [AppMenuItem]. */
@Composable
fun AppMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier.widthIn(min = MenuWidth),
        offset = offset,
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        content = content,
    )
}

/** Menu row, 48 high. Danger is red text and icon, never the default. */
@Composable
fun AppMenuItem(
    label: String,
    onClick: () -> Unit,
    @DrawableRes icon: Int? = null,
    danger: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val color = if (danger) cs.error else cs.onSurface
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        onClick = onClick,
        leadingIcon = icon?.let { { Icon(painterResource(it), null, Modifier.size(GymBuddyTheme.dimens.iconSmall)) } },
        colors = MenuDefaults.itemColors(textColor = color, leadingIconColor = color),
        contentPadding = PaddingValues(horizontal = GymBuddyTheme.dimens.group),
    )
}
