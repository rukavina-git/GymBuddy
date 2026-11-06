package com.rukavina.gymbuddy.ui.you

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.rukavina.gymbuddy.ui.design.AppBottomSheet
import com.rukavina.gymbuddy.ui.design.focusRing
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

/** Rows shared by the You page and Profile (R15-2, R15-3). */

/** Muted group label, then one card holding its rows (R15-2). */
@Composable
internal fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    val d = GymBuddyTheme.dimens
    Column(Modifier.padding(top = d.stack), verticalArrangement = Arrangement.spacedBy(d.stackTight)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = d.hairline).semantics { heading() },
        )
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
    }
}

@Composable
internal fun RowDivider() {
    HorizontalDivider(
        Modifier.padding(horizontal = GymBuddyTheme.dimens.insetCard),
        thickness = GymBuddyTheme.dimens.strokeDivider,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/** Title, optional subtitle, optional value, chevron. [danger] = red title, no chevron. */
@Composable
internal fun ValueRow(
    title: String,
    value: String?,
    onClick: () -> Unit,
    subtitle: String? = null,
    danger: Boolean = false,
) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        color = cs.surfaceContainer,
        shape = RectangleShape,
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().focusRing(interaction, RectangleShape),
    ) {
        Row(
            Modifier.heightIn(min = d.control).padding(horizontal = d.insetCard, vertical = d.stackTight),
            horizontalArrangement = Arrangement.spacedBy(d.stackTight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = if (danger) cs.error else cs.onSurface)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            if (!danger) Icon(painterResource(AppIcons.Ui.ChevronRight), null, Modifier.size(d.iconSmall), tint = cs.onSurfaceVariant)
        }
    }
}

/** One radio choice: [label], optional [subtitle]. */
internal data class Choice<out T>(val value: T, val label: String, val subtitle: String? = null)

/** Sheet of radio rows; tapping one picks it (R15-3 "editing in sheets"). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> ChoiceSheet(
    title: String,
    choices: List<Choice<T>>,
    current: T,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val d = GymBuddyTheme.dimens
    val c = GymBuddyTheme.colors
    val cs = MaterialTheme.colorScheme
    AppBottomSheet(onDismiss = onDismiss, title = title) {
        choices.forEach { choice ->
            val selected = choice.value == current
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = d.control)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onPick(choice.value) }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(d.stack),
            ) {
                RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = c.cobalt, unselectedColor = cs.outline))
                Column(Modifier.weight(1f)) {
                    Text(choice.label, style = MaterialTheme.typography.bodyLarge)
                    if (choice.subtitle != null) {
                        Text(choice.subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
