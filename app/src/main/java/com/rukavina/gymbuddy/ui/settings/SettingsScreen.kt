package com.rukavina.gymbuddy.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rukavina.gymbuddy.navigation.NavRoutes
import com.rukavina.gymbuddy.ui.components.ScreenHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    bottomNavController: NavHostController,
    rootNavController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("English") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Refresh profile data when screen resumes
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Observe logout event and navigate to login
    LaunchedEffect(Unit) {
        viewModel.logoutEvent.collect {
            rootNavController.navigate(NavRoutes.Login) {
                popUpTo(NavRoutes.Main) { inclusive = true }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                ScreenHeader(
                    title = "SETTINGS"
                )
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                item {
                    ProfileHeaderCard(
                        name = uiState.userName,
                        email = uiState.userEmail,
                        profileImageUri = uiState.profileImageUri,
                        onClick = {
                            bottomNavController.navigate(NavRoutes.Profile)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    SettingsSection {
                        SettingsItemWithValue(
                            icon = androidx.compose.material.icons.Icons.Default.Language,
                            label = "Language",
                            value = selectedLanguage,
                            onClick = {
                                showLanguageDialog = true
                            },
                            showDivider = true
                        )
                        SettingsItem(
                            icon = androidx.compose.material.icons.Icons.Default.Settings,
                            label = "Units",
                            onClick = {
                                bottomNavController.navigate(NavRoutes.EditUnits)
                            },
                            showDivider = true
                        )
                        SettingsItem(
                            icon = androidx.compose.material.icons.Icons.Default.Tune,
                            label = "App Preferences",
                            onClick = {
                                bottomNavController.navigate(NavRoutes.AppPreferences)
                            },
                            showDivider = true
                        )
                        SettingsItem(
                            icon = androidx.compose.material.icons.Icons.Default.VisibilityOff,
                            label = "Hidden Exercises",
                            onClick = {
                                bottomNavController.navigate(NavRoutes.HiddenExercises)
                            },
                            showDivider = true
                        )
                        SettingsItem(
                            icon = androidx.compose.material.icons.Icons.Default.FileDownload,
                            label = "Export Data",
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("This feature is not implemented yet")
                                }
                            },
                            showDivider = false
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    SettingsSection {
                        SettingsItem(
                            icon = androidx.compose.material.icons.Icons.Default.Info,
                            label = "About",
                            onClick = {
                                bottomNavController.navigate(NavRoutes.About)
                            },
                            showDivider = false
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Log Out", color = Color.White)
                    }
                }
            }
        }
        }

        // Logout confirmation dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Log Out") },
                text = { Text("Are you sure you want to log out?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            viewModel.logout()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Log Out")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Language selection dialog
        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguage = selectedLanguage,
                onLanguageSelected = { language ->
                    selectedLanguage = language
                    showLanguageDialog = false
                },
                onDismiss = { showLanguageDialog = false }
            )
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf("English") // For now, only English is available

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Language") },
        text = {
            Column {
                languages.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(language) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = language,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (language == currentLanguage) {
                            androidx.compose.material3.RadioButton(
                                selected = true,
                                onClick = null
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
