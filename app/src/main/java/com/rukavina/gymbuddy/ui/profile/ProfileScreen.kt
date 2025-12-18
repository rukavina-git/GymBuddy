package com.rukavina.gymbuddy.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.rukavina.gymbuddy.navigation.NavRoutes
import com.rukavina.gymbuddy.ui.settings.SettingsItem
import com.rukavina.gymbuddy.ui.settings.SettingsSection
import com.rukavina.gymbuddy.utils.UnitConverter
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    rootNavController: NavHostController,
    bottomNavController: NavHostController? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            // Take persistent URI permission
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
            viewModel.onImageSelected(it.toString())
            viewModel.onSaveClick()
        }
    }

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
                        profileImageUri = uiState.profileImageUri,
                        onImageClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
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

                        ProfileInfoItem(
                            icon = Icons.Default.Person,
                            label = "Age",
                            value = if (uiState.age.isNotBlank()) "${uiState.age} years" else "Not set",
                            onClick = {
                                bottomNavController?.navigate(NavRoutes.EditAge)
                            }
                        )

                        ProfileInfoItem(
                            icon = Icons.Default.MonitorWeight,
                            label = "Weight",
                            value = if (uiState.weight.isNotBlank()) "${uiState.weight} $weightUnit" else "Not set",
                            onClick = {
                                bottomNavController?.navigate(NavRoutes.EditWeight)
                            }
                        )

                        ProfileInfoItem(
                            icon = Icons.Default.MonitorWeight,
                            label = "Height",
                            value = if (uiState.height.isNotBlank()) {
                                if (uiState.preferredUnits == com.rukavina.gymbuddy.data.model.PreferredUnits.IMPERIAL) {
                                    val totalInches = uiState.height.toFloatOrNull()?.toInt() ?: 0
                                    val feet = totalInches / 12
                                    val inches = totalInches % 12
                                    "$feet'$inches\""
                                } else {
                                    "${uiState.height} $heightUnit"
                                }
                            } else "Not set",
                            onClick = {
                                bottomNavController?.navigate(NavRoutes.EditHeight)
                            }
                        )

                        ProfileInfoItem(
                            icon = Icons.Default.FitnessCenter,
                            label = "Target Weight",
                            value = if (uiState.targetWeight.isNotBlank()) "${uiState.targetWeight} $weightUnit" else "Not set",
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
    bio: String,
    profileImageUri: String?,
    onImageClick: () -> Unit
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
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .clickable(onClick = onImageClick),
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUri != null) {
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = "Profile image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default icon",
                        modifier = Modifier.size(50.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onImageClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit profile picture",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
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
