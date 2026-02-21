package com.rukavina.gymbuddy.ui.auth

import android.util.Log
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.rukavina.gymbuddy.BuildConfig
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.components.AppSnackbar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val tag = "LoginScreen"
    val authViewModel: AuthViewModel = viewModel()
    var isPasswordVisible by remember { mutableStateOf(false) }

    val isDebug = BuildConfig.DEBUG

    var email by remember { mutableStateOf(if (isDebug) "test001@gmail.com" else "") }
    var password by remember { mutableStateOf(if (isDebug) "Test.1234567" else "") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isGoogleSignInLoading by remember { mutableStateOf(false) }

    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val snackbarHostState by remember { mutableStateOf(SnackbarHostState()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val focusManager = LocalFocusManager.current
        Text(
            text = stringResource(R.string.welcome_auth),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = email,
            onValueChange = { email = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .focusRequester(emailFocusRequester),
            singleLine = true,
            placeholder = { Text(text = "Email") },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        PasswordToggleTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            isPasswordVisible = isPasswordVisible,
            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .focusRequester(passwordFocusRequester),
            focusDirection = FocusDirection.Down,
            imeAction = ImeAction.Done
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    authViewModel.loginUser(email, password) { isSuccess, error ->
                        if (isSuccess) {
                            Log.d(tag, "Login successful")
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Login successful.")
                            }
                            onLoginSuccess()
                        } else {
                            if (error != null && error.contains("no user record")) {
                                Log.d(tag, "Login error. Please check your credentials.")
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Login failed. Please check your credentials.")
                                }
                            } else {
                                Log.d(tag, "Login error: Login failed. Please check your credentials.")
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Login failed. Please check your credentials.")
                                }
                            }
                        }
                    }
                } else {
                    Log.d(tag, "Login error: Email and password cannot be empty.")
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Email and password cannot be empty.")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Log In")
        }

        Spacer(modifier = Modifier.height(8.dp))

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

        val webClientId = stringResource(R.string.default_web_client_id)

        OutlinedButton(
            onClick = {
                isGoogleSignInLoading = true
                coroutineScope.launch {
                    try {
                        val credentialManager = CredentialManager.create(context)
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(webClientId)
                            .build()

                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        val result = credentialManager.getCredential(context, request)
                        val credential = result.credential
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken

                        authViewModel.signInWithGoogle(idToken) { isSuccess, error ->
                            isGoogleSignInLoading = false
                            if (isSuccess) {
                                Log.d(tag, "Google Sign-In successful")
                                onLoginSuccess()
                            } else {
                                Log.e(tag, "Google Sign-In failed: $error")
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Google Sign-In failed: $error")
                                }
                            }
                        }
                    } catch (e: GetCredentialCancellationException) {
                        isGoogleSignInLoading = false
                        Log.d(tag, "Google Sign-In cancelled")
                    } catch (e: Exception) {
                        isGoogleSignInLoading = false
                        Log.e(tag, "Google Sign-In failed: ${e.message}")
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Google Sign-In failed: ${e.message}")
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isGoogleSignInLoading,
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
                Text(text = if (isGoogleSignInLoading) "Signing in..." else "Continue with Google")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { onNavigateToRegister() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Don't have an account? Sign up")
        }
    }

    AppSnackbar(
        snackbarHostState = snackbarHostState,
        modifier = Modifier.padding(16.dp),
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
