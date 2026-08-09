package com.rukavina.gymbuddy.ui.profile.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.rukavina.gymbuddy.ui.components.DateWheelPicker
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBirthdateScreen(
    navController: NavHostController,
    currentBirthDate: Long?,
    onSave: (Long) -> Unit
) {
    // Convert millis to LocalDate for the picker
    val initialDate = currentBirthDate?.let {
        Instant.ofEpochMilli(it)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    } ?: LocalDate.now().minusYears(25)

    var selectedDate by remember { mutableStateOf(initialDate) }

    // Date bounds
    val currentYear = LocalDate.now().year

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Birthdate") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Select your birthdate",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Custom Wheel Date Picker
            DateWheelPicker(
                selectedDate = selectedDate,
                onDateChanged = { selectedDate = it },
                minYear = 1920,
                maxYear = currentYear - 13
            )

            // Age preview
            val age = calculateAge(selectedDate)
            Text(
                text = "You are $age years old",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val millis = selectedDate
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    onSave(millis)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}

private fun calculateAge(birthDate: LocalDate): Int {
    val today = LocalDate.now()
    return Period.between(birthDate, today).years
}
