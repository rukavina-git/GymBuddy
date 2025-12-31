package com.rukavina.gymbuddy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Generic filter chip group for multi-select filtering.
 * Can be used with any enum or data class.
 *
 * @param T The type of items to display (typically an enum)
 * @param items List of all available items
 * @param selectedItems Set of currently selected items
 * @param onSelectionChange Callback when selection changes
 * @param itemLabel Function to get display label for each item
 * @param title Title for the filter group
 * @param modifier Modifier to be applied to the column
 */
@Composable
fun <T> FilterChipGroup(
    items: List<T>,
    selectedItems: Set<T>,
    onSelectionChange: (Set<T>) -> Unit,
    itemLabel: (T) -> String,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "$title (${if (selectedItems.isEmpty()) "All" else selectedItems.size})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                FilterChip(
                    selected = selectedItems.contains(item),
                    onClick = {
                        onSelectionChange(
                            if (selectedItems.contains(item)) {
                                selectedItems - item
                            } else {
                                selectedItems + item
                            }
                        )
                    },
                    label = { Text(itemLabel(item)) }
                )
            }
        }
    }
}
