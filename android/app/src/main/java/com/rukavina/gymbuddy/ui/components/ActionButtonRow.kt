package com.rukavina.gymbuddy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Generic action button row with primary and secondary actions.
 * Typically used in dialogs and bottom sheets with Clear/Apply or Cancel/Save patterns.
 *
 * @param onPrimaryAction Callback for the primary (filled) button
 * @param primaryActionText Text for the primary button
 * @param onSecondaryAction Callback for the secondary (outlined) button
 * @param secondaryActionText Text for the secondary button
 * @param modifier Modifier to be applied to the row
 * @param primaryEnabled Whether the primary button is enabled
 * @param secondaryEnabled Whether the secondary button is enabled
 */
@Composable
fun ActionButtonRow(
    onPrimaryAction: () -> Unit,
    primaryActionText: String,
    onSecondaryAction: () -> Unit,
    secondaryActionText: String,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    secondaryEnabled: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onSecondaryAction,
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
            enabled = secondaryEnabled
        ) {
            Text(secondaryActionText)
        }

        Button(
            onClick = onPrimaryAction,
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
            enabled = primaryEnabled
        ) {
            Text(primaryActionText)
        }
    }
}
