package com.rukavina.gymbuddy.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.components.AppSnackbar

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val webClientId = stringResource(R.string.default_web_client_id)

    // Handle one-time events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.LoginSuccess -> onLoginSuccess()
                is LoginEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // Login failed dialog
    if (uiState.showLoginFailedDialog) {
        LoginFailedDialog(
            onSignInWithGoogle = { viewModel.signInWithGoogleFromFailedDialog(context, webClientId) },
            onDismiss = { viewModel.dismissLoginFailedDialog() }
        )
    }

    // Account linking dialog
    if (uiState.linkDialog.isVisible) {
        AccountLinkingDialog(
            email = uiState.linkDialog.email,
            password = uiState.linkDialog.password,
            isPasswordVisible = uiState.linkDialog.isPasswordVisible,
            onPasswordChange = { viewModel.updateLinkPassword(it) },
            onTogglePasswordVisibility = { viewModel.toggleLinkPasswordVisibility() },
            onConfirm = { viewModel.loginAndLinkGoogle() },
            onDismiss = { viewModel.dismissLinkDialog() }
        )
    }

    // Main content
    LoginContent(
        email = uiState.form.email,
        password = uiState.form.password,
        isPasswordVisible = uiState.form.isPasswordVisible,
        isLoginLoading = uiState.loading.isLoginLoading,
        isGoogleSignInLoading = uiState.loading.isGoogleSignInLoading,
        onEmailChange = { viewModel.updateEmail(it) },
        onPasswordChange = { viewModel.updatePassword(it) },
        onTogglePasswordVisibility = { viewModel.togglePasswordVisibility() },
        onLoginClick = { viewModel.loginWithEmailPassword() },
        onGoogleSignInClick = { viewModel.signInWithGoogle(context, webClientId) },
        onNavigateToRegister = onNavigateToRegister
    )

    AppSnackbar(
        snackbarHostState = snackbarHostState,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun LoginContent(
    email: String,
    password: String,
    isPasswordVisible: Boolean,
    isLoginLoading: Boolean,
    isGoogleSignInLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLoginClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.welcome_auth),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            singleLine = true,
            placeholder = { Text(text = "Email") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        // Password field
        PasswordToggleTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            isPasswordVisible = isPasswordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            focusDirection = FocusDirection.Down,
            imeAction = ImeAction.Done
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Login button
        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoginLoading
        ) {
            Text(text = if (isLoginLoading) "Signing in..." else "Log In")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "  or  ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Google sign-in button
        GoogleSignInButton(
            isLoading = isGoogleSignInLoading,
            onClick = onGoogleSignInClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Register link
        TextButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Don't have an account? Sign up")
        }
    }
}

@Composable
private fun GoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_google),
                contentDescription = "Google",
                modifier = Modifier.size(18.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (isLoading) "Signing in..." else "Continue with Google")
        }
    }
}

@Composable
private fun LoginFailedDialog(
    onSignInWithGoogle: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Login Failed") },
        text = {
            Text(
                "This email may be connected to a Google account. Try signing in with Google instead. You can add a password later in Settings.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            OutlinedButton(
                onClick = onSignInWithGoogle,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google",
                        modifier = Modifier.size(18.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign in with Google")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Try Again")
            }
        }
    )
}

@Composable
private fun AccountLinkingDialog(
    email: String,
    password: String,
    isPasswordVisible: Boolean,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link Your Account") },
        text = {
            Column {
                Text(
                    "An account with $email already exists. Enter your password to link your Google account.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                PasswordToggleTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = "Password",
                    isPasswordVisible = isPasswordVisible,
                    onTogglePasswordVisibility = onTogglePasswordVisibility,
                    modifier = Modifier.fillMaxWidth(),
                    focusDirection = FocusDirection.Down,
                    imeAction = ImeAction.Done
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Link & Sign In")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        onLoginSuccess = {},
        onNavigateToRegister = {}
    )
}
