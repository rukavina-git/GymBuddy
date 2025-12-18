package com.rukavina.gymbuddy.ui.profile.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.rukavina.gymbuddy.data.model.PreferredUnits
import com.rukavina.gymbuddy.ui.components.DecimalNumberPicker
import com.rukavina.gymbuddy.ui.components.NumberPicker
import com.rukavina.gymbuddy.utils.UnitConverter

data class PhysicalInfo(
    val age: String,
    val weight: String,
    val height: String,
    val targetWeight: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPhysicalInfoScreen(
    navController: NavHostController,
    currentInfo: PhysicalInfo,
    preferredUnits: PreferredUnits,
    onSave: (PhysicalInfo) -> Unit
) {
    var age by remember {
        mutableIntStateOf(currentInfo.age.toIntOrNull() ?: 25)
    }

    val isMetric = preferredUnits == PreferredUnits.METRIC

    var weight by remember {
        mutableFloatStateOf(currentInfo.weight.toFloatOrNull() ?: if (isMetric) 70f else 154f)
    }

    var height by remember {
        mutableFloatStateOf(currentInfo.height.toFloatOrNull() ?: if (isMetric) 170f else 67f)
    }

    var targetWeight by remember {
        mutableFloatStateOf(currentInfo.targetWeight.toFloatOrNull() ?: if (isMetric) 70f else 154f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Physical Info") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Age Picker
            PickerCard(title = "Age") {
                NumberPicker(
                    value = age,
                    onValueChange = { age = it },
                    range = 13..100,
                    label = "years",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Weight Picker
            val weightUnit = UnitConverter.getWeightUnitLabel(preferredUnits)
            val weightRange = if (isMetric) 30f..200f else 66f..440f

            PickerCard(title = "Weight") {
                DecimalNumberPicker(
                    value = weight,
                    onValueChange = { weight = it },
                    range = weightRange,
                    step = if (isMetric) 0.1f else 0.5f,
                    label = weightUnit,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Height Picker
            val heightUnit = UnitConverter.getHeightUnitLabel(preferredUnits)
            val heightRange = if (isMetric) 100f..250f else 39f..98f

            PickerCard(title = "Height") {
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
                    // For imperial, convert inches to feet'inches display
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
            }

            // Target Weight Picker
            PickerCard(title = "Target Weight") {
                DecimalNumberPicker(
                    value = targetWeight,
                    onValueChange = { targetWeight = it },
                    range = weightRange,
                    step = if (isMetric) 0.1f else 0.5f,
                    label = weightUnit,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    onSave(
                        PhysicalInfo(
                            age = age.toString(),
                            weight = "%.1f".format(weight),
                            height = if (isMetric) "%.0f".format(height) else "%.0f".format(height),
                            targetWeight = "%.1f".format(targetWeight)
                        )
                    )
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun PickerCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
