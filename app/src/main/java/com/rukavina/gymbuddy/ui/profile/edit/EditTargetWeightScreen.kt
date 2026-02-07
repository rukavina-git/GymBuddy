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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.rukavina.gymbuddy.Constants
import com.rukavina.gymbuddy.data.model.PreferredUnits
import com.rukavina.gymbuddy.ui.components.DecimalNumberPicker
import com.rukavina.gymbuddy.utils.UnitConverter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTargetWeightScreen(
    navController: NavHostController,
    currentTargetWeight: Float, // Always in kg from database
    preferredUnits: PreferredUnits,
    onSave: (Float) -> Unit // Always saves in kg to database
) {
    val isMetric = preferredUnits == PreferredUnits.METRIC

    // Weight is always stored in kg internally
    var weightKg by remember {
        mutableFloatStateOf(
            currentTargetWeight.coerceIn(
                Constants.Measurements.MIN_WEIGHT_KG.toFloat(),
                Constants.Measurements.MAX_WEIGHT_KG.toFloat()
            )
        )
    }

    // Display value in user's preferred units
    val displayWeight = if (isMetric) weightKg else UnitConverter.kgToLbs(weightKg)
    val weightUnit = UnitConverter.getWeightUnitLabel(preferredUnits)

    // Calculate display range based on kg limits
    val displayRange = if (isMetric) {
        Constants.Measurements.MIN_WEIGHT_KG.toFloat()..Constants.Measurements.MAX_WEIGHT_KG.toFloat()
    } else {
        UnitConverter.kgToLbs(Constants.Measurements.MIN_WEIGHT_KG.toFloat())..
                UnitConverter.kgToLbs(Constants.Measurements.MAX_WEIGHT_KG.toFloat())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Target Weight") },
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
                text = "Select your target weight",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DecimalNumberPicker(
                value = displayWeight,
                onValueChange = { newDisplayWeight ->
                    // Convert back to kg for internal storage
                    weightKg = if (isMetric) {
                        newDisplayWeight
                    } else {
                        UnitConverter.lbsToKg(newDisplayWeight)
                    }.coerceIn(
                        Constants.Measurements.MIN_WEIGHT_KG.toFloat(),
                        Constants.Measurements.MAX_WEIGHT_KG.toFloat()
                    )
                },
                range = displayRange,
                step = if (isMetric) 0.1f else 0.5f,
                label = weightUnit,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // Always save in kg
                    onSave(weightKg)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
