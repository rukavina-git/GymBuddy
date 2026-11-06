package com.rukavina.gymbuddy.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rukavina.gymbuddy.navigation.NavRoutes
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.utils.UnitConverter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    rootNavController: NavHostController,
    bottomNavController: NavHostController? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    bottomNavController?.let {
                        IconButton(onClick = { it.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ProfileHeader(
                        name = uiState.name.ifBlank { "Guest User" },
                        email = uiState.email,
                        bio = uiState.bio,
                    )
                }

                item {
                    SettingsSection {
                        ProfileInfoItem(
                            icon = Icons.Default.Person,
                            label = "Name",
                            value = uiState.name.ifBlank { "Not set" },
                            onClick = {
                                bottomNavController?.navigate(NavRoutes.EditName)
                            }
                        )

                        ProfileInfoItem(
                            icon = androidx.compose.material.icons.Icons.Default.Email,
                            label = "Email",
                            value = uiState.email,
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("This feature is not implemented yet")
                                }
                            },
                            showDivider = uiState.bio.isNotBlank()
                        )

                        if (uiState.bio.isNotBlank()) {
                            ProfileInfoItem(
                                icon = Icons.Default.Notes,
                                label = "Bio",
                                value = uiState.bio,
                                onClick = {
                                    bottomNavController?.navigate(NavRoutes.EditBio)
                                },
                                showDivider = false
                            )
                        }
                    }
                }

                item {
                    SettingsSection {
                        val weightUnit = UnitConverter.getWeightUnitLabel(uiState.preferredUnits)
                        val heightUnit = UnitConverter.getHeightUnitLabel(uiState.preferredUnits)
                        val displayWeight = UnitConverter.weightToDisplayUnit(uiState.weight, uiState.preferredUnits)
                        val displayTargetWeight = UnitConverter.weightToDisplayUnit(uiState.targetWeight, uiState.preferredUnits)

                        ProfileInfoItem(
                            icon = Icons.Default.Person,
                            label = "Age",
                            value = viewModel.calculateAge(uiState.birthDate)?.let { "$it years" } ?: "Not set",
                            onClick = {
                                bottomNavController?.navigate(NavRoutes.EditBirthdate)
                            }
                        )

                        ProfileInfoItem(
                            icon = Icons.Default.MonitorWeight,
                            label = "Weight",
                            value = if (displayWeight.isNotBlank()) "$displayWeight $weightUnit" else "Not set",
                            onClick = {
                                bottomNavController?.navigate(NavRoutes.EditWeight)
                            }
                        )

                        val heightCm = uiState.height
                        ProfileInfoItem(
                            icon = Icons.Default.MonitorWeight,
                            label = "Height",
                            value = if (heightCm != null) {
                                if (uiState.preferredUnits == PreferredUnits.IMPERIAL) {
                                    val totalInches = UnitConverter.cmToInches(heightCm)
                                    val feet = (totalInches / 12).toInt()
                                    val inches = kotlin.math.round(totalInches % 12).toInt()
                                    "$feet'$inches\""
                                } else {
                                    "${UnitConverter.heightToDisplayUnit(heightCm, uiState.preferredUnits)} $heightUnit"
                                }
                            } else "Not set",
                            onClick = {
                                bottomNavController?.navigate(NavRoutes.EditHeight)
                            }
                        )

                        ProfileInfoItem(
                            icon = Icons.Default.FitnessCenter,
                            label = "Target Weight",
                            value = if (displayTargetWeight.isNotBlank()) "$displayTargetWeight $weightUnit" else "Not set",
                            onClick = {
                                bottomNavController?.navigate(NavRoutes.EditTargetWeight)
                            }
                        )

                        ProfileInfoItem(
                            icon = Icons.Default.Person,
                            label = "Gender",
                            value = uiState.gender?.name?.replace("_", " ") ?: "Not specified",
                            onClick = {
                                bottomNavController?.navigate(NavRoutes.EditGender)
                            }
                        )

                        ProfileInfoItem(
                            icon = Icons.Default.FitnessCenter,
                            label = "Fitness Goal",
                            value = uiState.fitnessGoal?.name?.replace("_", " ") ?: "Not set",
                            onClick = {
                                bottomNavController?.navigate(NavRoutes.EditFitnessGoal)
                            }
                        )

                        ProfileInfoItem(
                            icon = Icons.Default.SportsGymnastics,
                            label = "Activity Level",
                            value = uiState.activityLevel?.name?.replace("_", " ") ?: "Not specified",
                            onClick = {
                                bottomNavController?.navigate(NavRoutes.EditActivityLevel)
                            },
                            showDivider = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    name: String,
    email: String,
    bio: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(100.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                // Preset avatars replace this in roadmap 6.5; no custom photos in v1.
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

/** Old grouped card; goes with this screen in 7.10b. */
@Composable
private fun SettingsSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) { content() }
    }
}
