package com.rukavina.gymbuddy.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.rukavina.gymbuddy.data.model.ActivityLevel
import com.rukavina.gymbuddy.data.model.FitnessGoal
import com.rukavina.gymbuddy.data.model.Gender
import com.rukavina.gymbuddy.data.model.PreferredUnits
import com.rukavina.gymbuddy.navigation.NavRoutes
import com.rukavina.gymbuddy.utils.UnitConverter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    rootNavController: NavHostController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Intercept back button when there are unsaved changes
    BackHandler(enabled = viewModel.hasUnsavedChanges()) {
        showDiscardDialog = true
    }

    // Show message in snackbar
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Your Profile") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!uiState.isLoading) {
                Surface(
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { viewModel.onSaveClick() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Image
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .clickable { viewModel.onImageClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.profileImageUri != null) {
                        AsyncImage(
                            model = uiState.profileImageUri,
                            contentDescription = "Profile image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default icon",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Name (required)
                ProfileTextField("Name *", uiState.name) {
                    viewModel.onFieldChanged(ProfileField.Name, it)
                }

                // Email (read-only)
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = {},
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    readOnly = true
                )

                // Age
                ProfileTextField("Age", uiState.age, KeyboardType.Number) {
                    viewModel.onFieldChanged(ProfileField.Age, it)
                }

                // Units selector
                PreferredUnitsDropdown(
                    selectedUnits = uiState.preferredUnits,
                    onUnitsSelected = { viewModel.onPreferredUnitsChanged(it) }
                )

                // Weight
                val weightUnit = UnitConverter.getWeightUnitLabel(uiState.preferredUnits)
                ProfileTextField("Weight ($weightUnit)", uiState.weight, KeyboardType.Number) {
                    viewModel.onFieldChanged(ProfileField.Weight, it)
                }

                // Height
                val heightUnit = UnitConverter.getHeightUnitLabel(uiState.preferredUnits)
                ProfileTextField("Height ($heightUnit)", uiState.height, KeyboardType.Number) {
                    viewModel.onFieldChanged(ProfileField.Height, it)
                }

                // Target Weight
                ProfileTextField("Target Weight ($weightUnit)", uiState.targetWeight, KeyboardType.Number) {
                    viewModel.onFieldChanged(ProfileField.TargetWeight, it)
                }

                // Gender dropdown
                GenderDropdown(
                    selectedGender = uiState.gender,
                    onGenderSelected = { viewModel.onGenderChanged(it) }
                )

                // Fitness Goal dropdown (required)
                FitnessGoalDropdown(
                    selectedGoal = uiState.fitnessGoal,
                    onGoalSelected = { viewModel.onFitnessGoalChanged(it) }
                )

                // Activity Level dropdown
                ActivityLevelDropdown(
                    selectedLevel = uiState.activityLevel,
                    onLevelSelected = { viewModel.onActivityLevelChanged(it) }
                )

                // Bio (multiline)
                OutlinedTextField(
                    value = uiState.bio,
                    onValueChange = { viewModel.onFieldChanged(ProfileField.Bio, it) },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5
                )
            }
        }
    }

    // Discard confirmation dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Changes?") },
            text = {
                Text("Are you sure you want to discard your changes? All unsaved changes will be lost.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onCancelClick()
                        showDiscardDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GenderDropdown(
    selectedGender: Gender?,
    onGenderSelected: (Gender?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedGender?.name?.replace("_", " ") ?: "",
            onValueChange = {},
            label = { Text("Gender") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, "Dropdown")
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Not specified") },
                onClick = {
                    onGenderSelected(null)
                    expanded = false
                }
            )
            Gender.entries.forEach { gender ->
                DropdownMenuItem(
                    text = { Text(gender.name.replace("_", " ")) },
                    onClick = {
                        onGenderSelected(gender)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun FitnessGoalDropdown(
    selectedGoal: FitnessGoal?,
    onGoalSelected: (FitnessGoal?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedGoal?.name?.replace("_", " ") ?: "",
            onValueChange = {},
            label = { Text("Fitness Goal *") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, "Dropdown")
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            FitnessGoal.entries.forEach { goal ->
                DropdownMenuItem(
                    text = { Text(goal.name.replace("_", " ")) },
                    onClick = {
                        onGoalSelected(goal)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ActivityLevelDropdown(
    selectedLevel: ActivityLevel?,
    onLevelSelected: (ActivityLevel?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedLevel?.name?.replace("_", " ") ?: "",
            onValueChange = {},
            label = { Text("Activity Level") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, "Dropdown")
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Not specified") },
                onClick = {
                    onLevelSelected(null)
                    expanded = false
                }
            )
            ActivityLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = { Text(level.name.replace("_", " ")) },
                    onClick = {
                        onLevelSelected(level)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PreferredUnitsDropdown(
    selectedUnits: PreferredUnits,
    onUnitsSelected: (PreferredUnits) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
        OutlinedTextField(
            value = selectedUnits.name,
            onValueChange = {},
            label = { Text("Preferred Units") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, "Dropdown")
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            PreferredUnits.entries.forEach { units ->
                DropdownMenuItem(
                    text = { Text(units.name) },
                    onClick = {
                        onUnitsSelected(units)
                        expanded = false
                    }
                )
            }
        }
    }
}

