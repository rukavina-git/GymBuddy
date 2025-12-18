package com.rukavina.gymbuddy.ui.profile.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.rukavina.gymbuddy.data.model.PreferredUnits
import com.rukavina.gymbuddy.ui.components.DecimalNumberPicker
import com.rukavina.gymbuddy.ui.components.NumberPicker
import com.rukavina.gymbuddy.utils.UnitConverter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHeightScreen(
    navController: NavHostController,
    currentHeight: Float,
    preferredUnits: PreferredUnits,
    onSave: (Float) -> Unit
) {
    var height by remember { mutableFloatStateOf(currentHeight) }

    val isMetric = preferredUnits == PreferredUnits.METRIC
    val heightUnit = UnitConverter.getHeightUnitLabel(preferredUnits)
    val heightRange = if (isMetric) 100f..250f else 39f..98f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Height") },
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
                text = "Select your height",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isMetric) {
                DecimalNumberPicker(
                    value = height,
                    onValueChange = { height = it },
                    range = heightRange,
                    step = 1f,
                    label = heightUnit,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // For imperial, show feet and inches side by side
                val totalInches = height.toInt()
                val feet = totalInches / 12
                val inches = totalInches % 12

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NumberPicker(
                        value = feet,
                        onValueChange = { newFeet ->
                            height = (newFeet * 12 + inches).toFloat()
                        },
                        range = 3..8,
                        label = "ft",
                        modifier = Modifier.weight(1f)
                    )

                    NumberPicker(
                        value = inches,
                        onValueChange = { newInches ->
                            height = (feet * 12 + newInches).toFloat()
                        },
                        range = 0..11,
                        label = "in",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    onSave(height)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
