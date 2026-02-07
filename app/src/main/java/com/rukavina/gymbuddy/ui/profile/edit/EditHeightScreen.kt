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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.rukavina.gymbuddy.Constants
import com.rukavina.gymbuddy.data.model.PreferredUnits
import com.rukavina.gymbuddy.ui.components.NumberPicker
import com.rukavina.gymbuddy.utils.UnitConverter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHeightScreen(
    navController: NavHostController,
    currentHeight: Float, // Always in cm from database
    preferredUnits: PreferredUnits,
    onSave: (Float) -> Unit // Always saves in cm to database
) {
    val isMetric = preferredUnits == PreferredUnits.METRIC

    // Coerce initial height to valid range
    val initialHeightCm = currentHeight.roundToInt()
        .coerceIn(Constants.Measurements.MIN_HEIGHT_CM, Constants.Measurements.MAX_HEIGHT_CM)

    // For metric: single cm state (keyed to reinitialize if currentHeight changes)
    var heightCm by remember(currentHeight) { mutableIntStateOf(initialHeightCm) }

    // For imperial: independent feet and inches state (keyed to reinitialize if currentHeight changes)
    val initialTotalInches = UnitConverter.cmToInches(initialHeightCm.toFloat()).roundToInt()
    var feet by remember(currentHeight) { mutableIntStateOf(initialTotalInches / 12) }
    var inches by remember(currentHeight) { mutableIntStateOf(initialTotalInches % 12) }

    // Calculate min/max feet based on cm limits
    val minFeet = (UnitConverter.cmToInches(Constants.Measurements.MIN_HEIGHT_CM.toFloat()) / 12).toInt()
    val maxFeet = (UnitConverter.cmToInches(Constants.Measurements.MAX_HEIGHT_CM.toFloat()) / 12).toInt()

    // Calculate max inches when at max feet (to not exceed cm limit)
    val maxInchesAtMaxFeet = (UnitConverter.cmToInches(Constants.Measurements.MAX_HEIGHT_CM.toFloat()) % 12).toInt()
    // Calculate min inches when at min feet (to not go below cm limit)
    val minInchesAtMinFeet = kotlin.math.ceil(UnitConverter.cmToInches(Constants.Measurements.MIN_HEIGHT_CM.toFloat()) % 12).toInt()

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
                // Metric: simple cm picker
                NumberPicker(
                    value = heightCm,
                    onValueChange = { heightCm = it },
                    range = Constants.Measurements.MIN_HEIGHT_CM..Constants.Measurements.MAX_HEIGHT_CM,
                    label = "cm",
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Imperial: independent feet and inches pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NumberPicker(
                        value = feet,
                        onValueChange = { newFeet ->
                            feet = newFeet.coerceIn(minFeet, maxFeet)
                        },
                        range = minFeet..maxFeet,
                        label = "ft",
                        modifier = Modifier.weight(1f)
                    )

                    // Dynamic inches range based on current feet
                    val inchesMin = if (feet == minFeet) minInchesAtMinFeet else 0
                    val inchesMax = if (feet == maxFeet) maxInchesAtMaxFeet else 11

                    NumberPicker(
                        value = inches.coerceIn(inchesMin, inchesMax),
                        onValueChange = { newInches ->
                            inches = newInches.coerceIn(inchesMin, inchesMax)
                        },
                        range = inchesMin..inchesMax,
                        label = "in",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // Convert to cm and save
                    val finalHeightCm = if (isMetric) {
                        heightCm
                    } else {
                        val totalInches = feet * 12 + inches
                        UnitConverter.inchesToCm(totalInches.toFloat()).roundToInt()
                    }.coerceIn(Constants.Measurements.MIN_HEIGHT_CM, Constants.Measurements.MAX_HEIGHT_CM)

                    onSave(finalHeightCm.toFloat())
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
