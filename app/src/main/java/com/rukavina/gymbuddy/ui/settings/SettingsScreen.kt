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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.navigation.NavRoutes
import com.rukavina.gymbuddy.ui.auth.PasswordToggleTextField
import com.rukavina.gymbuddy.ui.components.ScreenHeader
import kotlinx.coroutines.launch

/**
 * State for password setup/change dialog.
 */
private data class PasswordDialogState(
    val isVisible: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isCurrentPasswordVisible: Boolean = false,
    val isNewPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val error: String = "",
    val isLoading: Boolean = false
)

/**
 * State for delete account dialog.
 */
private data class DeleteAccountDialogState(
    val isVisible: Boolean = false,
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val error: String = "",
    val isLoading: Boolean = false
)

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
    var passwordDialogState by remember { mutableStateOf(PasswordDialogState()) }
    var deleteAccountDialogState by remember { mutableStateOf(DeleteAccountDialogState()) }
    val context = LocalContext.current

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

    // Observe account deleted event and navigate to login
    LaunchedEffect(Unit) {
        viewModel.accountDeletedEvent.collect {
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
                            icon = Icons.Default.Language,
                            label = "Language",
                            value = selectedLanguage,
                            onClick = {
                                showLanguageDialog = true
                            },
                            showDivider = true
                        )
                        SettingsItem(
                            icon = Icons.Default.Settings,
                            label = "Units",
                            onClick = {
                                bottomNavController.navigate(NavRoutes.EditUnits)
                            },
                            showDivider = true
                        )
                        SettingsItem(
                            icon = Icons.Default.Tune,
                            label = "App Preferences",
                            onClick = {
                                bottomNavController.navigate(NavRoutes.AppPreferences)
                            },
                            showDivider = true
                        )
                        SettingsItem(
                            icon = Icons.Default.FileDownload,
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

                // Account section
                item {
                    SettingsSection {
                        SettingsItemWithValue(
                            icon = Icons.Default.Lock,
                            label = "Password",
                            value = if (uiState.hasPassword) "" else "Not set",
                            onClick = { passwordDialogState = passwordDialogState.copy(isVisible = true) },
                            showDivider = true
                        )
                        SettingsItem(
                            icon = Icons.Default.DeleteForever,
                            label = "Delete Account",
                            onClick = { deleteAccountDialogState = deleteAccountDialogState.copy(isVisible = true) },
                            showDivider = false,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    SettingsSection {
                        SettingsItem(
                            icon = Icons.Default.Info,
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
            LogoutConfirmationDialog(
                onConfirm = {
                    showLogoutDialog = false
                    viewModel.logout()
                },
                onDismiss = { showLogoutDialog = false }
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

        // Password setup/change dialog
        if (passwordDialogState.isVisible) {
            PasswordManagementDialog(
                state = passwordDialogState,
                isSetup = uiState.isGoogleOnly,
                onStateChange = { passwordDialogState = it },
                onSave = { isSetup, currentPwd, newPwd ->
                    passwordDialogState = passwordDialogState.copy(isLoading = true)
                    if (isSetup) {
                        viewModel.setupPassword(newPwd) { success, error ->
                            if (success) {
                                passwordDialogState = PasswordDialogState()
                                scope.launch {
                                    snackbarHostState.showSnackbar("Password set up successfully!")
                                }
                            } else {
                                passwordDialogState = passwordDialogState.copy(
                                    isLoading = false,
                                    error = error ?: "Failed to set password"
                                )
                            }
                        }
                    } else {
                        viewModel.changePassword(currentPwd, newPwd) { success, error ->
                            if (success) {
                                passwordDialogState = PasswordDialogState()
                                scope.launch {
                                    snackbarHostState.showSnackbar("Password changed successfully!")
                                }
                            } else {
                                passwordDialogState = passwordDialogState.copy(
                                    isLoading = false,
                                    error = error ?: "Failed to change password"
                                )
                            }
                        }
                    }
                },
                onValidate = { viewModel.isPasswordStrong(it) },
                onDismiss = { passwordDialogState = PasswordDialogState() }
            )
        }

        // Delete account dialog
        if (deleteAccountDialogState.isVisible) {
            DeleteAccountDialog(
                state = deleteAccountDialogState,
                isGoogleOnly = uiState.isGoogleOnly,
                onStateChange = { deleteAccountDialogState = it },
                onDeleteWithPassword = { password ->
                    deleteAccountDialogState = deleteAccountDialogState.copy(isLoading = true)
                    viewModel.deleteAccountWithPassword(password) { success, error ->
                        if (!success) {
                            deleteAccountDialogState = deleteAccountDialogState.copy(
                                isLoading = false,
                                error = error ?: "Failed to delete account"
                            )
                        }
                        // On success, the accountDeletedEvent will handle navigation
                    }
                },
                onDeleteWithGoogle = {
                    deleteAccountDialogState = deleteAccountDialogState.copy(isLoading = true)
                    val webClientId = context.getString(R.string.default_web_client_id)
                    viewModel.deleteAccountWithGoogle(context, webClientId) { success, error ->
                        if (!success) {
                            deleteAccountDialogState = deleteAccountDialogState.copy(
                                isLoading = false,
                                error = error ?: "Failed to delete account"
                            )
                        }
                        // On success, the accountDeletedEvent will handle navigation
                    }
                },
                onDismiss = { deleteAccountDialogState = DeleteAccountDialogState() }
            )
        }
    }
}

@Composable
private fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Out") },
        text = { Text("Are you sure you want to log out?") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Log Out")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LanguageSelectionDialog(
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
                            RadioButton(
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

@Composable
private fun PasswordManagementDialog(
    state: PasswordDialogState,
    isSetup: Boolean,
    onStateChange: (PasswordDialogState) -> Unit,
    onSave: (isSetup: Boolean, currentPassword: String, newPassword: String) -> Unit,
    onValidate: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    val dialogTitle = if (isSetup) "Set Up Password" else "Change Password"

    AlertDialog(
        onDismissRequest = { if (!state.isLoading) onDismiss() },
        title = { Text(dialogTitle) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.error.isNotEmpty()) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Current password field (only for change, not setup)
                if (!isSetup) {
                    PasswordToggleTextField(
                        value = state.currentPassword,
                        onValueChange = { onStateChange(state.copy(currentPassword = it, error = "")) },
                        label = "Current Password",
                        isPasswordVisible = state.isCurrentPasswordVisible,
                        onTogglePasswordVisibility = {
                            onStateChange(state.copy(isCurrentPasswordVisible = !state.isCurrentPasswordVisible))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        focusDirection = FocusDirection.Down,
                        imeAction = ImeAction.Next
                    )
                }

                PasswordToggleTextField(
                    value = state.newPassword,
                    onValueChange = { onStateChange(state.copy(newPassword = it, error = "")) },
                    label = if (isSetup) "Password" else "New Password",
                    isPasswordVisible = state.isNewPasswordVisible,
                    onTogglePasswordVisibility = {
                        onStateChange(state.copy(isNewPasswordVisible = !state.isNewPasswordVisible))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    focusDirection = FocusDirection.Down,
                    imeAction = ImeAction.Next
                )

                PasswordToggleTextField(
                    value = state.confirmPassword,
                    onValueChange = { onStateChange(state.copy(confirmPassword = it, error = "")) },
                    label = "Confirm Password",
                    isPasswordVisible = state.isConfirmPasswordVisible,
                    onTogglePasswordVisibility = {
                        onStateChange(state.copy(isConfirmPasswordVisible = !state.isConfirmPasswordVisible))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    focusDirection = FocusDirection.Down,
                    imeAction = ImeAction.Done
                )

                Text(
                    text = "Password must be at least 8 characters with uppercase, lowercase, and a number.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        !isSetup && state.currentPassword.isEmpty() -> {
                            onStateChange(state.copy(error = "Please enter current password"))
                        }
                        state.newPassword.isEmpty() || state.confirmPassword.isEmpty() -> {
                            onStateChange(state.copy(error = "Please fill in all fields"))
                        }
                        state.newPassword != state.confirmPassword -> {
                            onStateChange(state.copy(error = "Passwords do not match"))
                        }
                        !onValidate(state.newPassword) -> {
                            onStateChange(state.copy(error = "Password does not meet requirements"))
                        }
                        else -> {
                            onSave(isSetup, state.currentPassword, state.newPassword)
                        }
                    }
                },
                enabled = !state.isLoading
            ) {
                Text(if (state.isLoading) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !state.isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteAccountDialog(
    state: DeleteAccountDialogState,
    isGoogleOnly: Boolean,
    onStateChange: (DeleteAccountDialogState) -> Unit,
    onDeleteWithPassword: (String) -> Unit,
    onDeleteWithGoogle: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!state.isLoading) onDismiss() },
        title = {
            Text(
                text = "Delete Account",
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "This action is permanent and cannot be undone. All your data will be deleted:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Column(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("• Profile information", style = MaterialTheme.typography.bodySmall)
                    Text("• Workout history", style = MaterialTheme.typography.bodySmall)
                    Text("• Custom exercises", style = MaterialTheme.typography.bodySmall)
                    Text("• All saved data", style = MaterialTheme.typography.bodySmall)
                }

                if (state.error.isNotEmpty()) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (!isGoogleOnly) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter your password to confirm:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    PasswordToggleTextField(
                        value = state.password,
                        onValueChange = { onStateChange(state.copy(password = it, error = "")) },
                        label = "Password",
                        isPasswordVisible = state.isPasswordVisible,
                        onTogglePasswordVisibility = {
                            onStateChange(state.copy(isPasswordVisible = !state.isPasswordVisible))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        focusDirection = FocusDirection.Down,
                        imeAction = ImeAction.Done
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You'll need to sign in with Google to confirm deletion.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Button(
                    onClick = {
                        if (isGoogleOnly) {
                            onDeleteWithGoogle()
                        } else {
                            if (state.password.isEmpty()) {
                                onStateChange(state.copy(error = "Please enter your password"))
                            } else {
                                onDeleteWithPassword(state.password)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (isGoogleOnly) "Sign in & Delete" else "Delete Account")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !state.isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}
