package com.rukavina.gymbuddy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun DateWheelPicker(
    selectedDate: LocalDate,
    onDateChanged: (LocalDate) -> Unit,
    minYear: Int = 1920,
    maxYear: Int = LocalDate.now().year - 13,
    modifier: Modifier = Modifier
) {
    var day by remember { mutableIntStateOf(selectedDate.dayOfMonth) }
    var month by remember { mutableIntStateOf(selectedDate.monthValue) }
    var year by remember { mutableIntStateOf(selectedDate.year) }

    // Calculate max days for the selected month/year
    val maxDays = remember(month, year) {
        YearMonth.of(year, month).lengthOfMonth()
    }

    // Adjust day if it exceeds max days for the month
    LaunchedEffect(maxDays) {
        if (day > maxDays) {
            day = maxDays
        }
    }

    // Notify parent of date changes
    LaunchedEffect(day, month, year) {
        val validDay = day.coerceIn(1, maxDays)
        val newDate = LocalDate.of(year, month, validDay)
        if (newDate != selectedDate) {
            onDateChanged(newDate)
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day picker
        NumberPicker(
            value = day,
            onValueChange = { day = it },
            range = 1..maxDays,
            modifier = Modifier.width(70.dp)
        )

        // Month picker
        MonthPicker(
            value = month,
            onValueChange = { month = it },
            modifier = Modifier.width(110.dp)
        )

        // Year picker
        NumberPicker(
            value = year,
            onValueChange = { year = it },
            range = minYear..maxYear,
            modifier = Modifier.width(90.dp)
        )
    }
}

@Composable
private fun MonthPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    StringWheelPicker(
        items = months,
        selectedIndex = value - 1,
        onSelectedIndexChange = { onValueChange(it + 1) },
        modifier = modifier
    )
}
