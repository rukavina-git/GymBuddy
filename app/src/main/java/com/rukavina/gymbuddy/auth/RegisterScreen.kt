package com.rukavina.gymbuddy.auth

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.common.AppSnackbar
import com.rukavina.gymbuddy.navigation.NavigationActions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(navController: NavHostController) {
    val tag = "RegistrationScreen"
    val authViewModel: AuthViewModel = viewModel()

    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var isEmailValid by remember { mutableStateOf(true) }
    var isUsernameValid by remember { mutableStateOf(true) }
    var passwordStrengthMessage by remember { mutableStateOf("") }
    var doPasswordsMatch by remember { mutableStateOf(true) }
    val passwordFocusRequester = remember { FocusRequester() }

    val snackbarHostState by remember { mutableStateOf(SnackbarHostState()) }
    val coroutineScope = rememberCoroutineScope()

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

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                if (!isEmailValid) {
                    Text(
                        text = "Invalid email format",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(start = 8.dp)
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        isEmailValid = isEmailValid(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .border(
                            width = 1.dp,
                            color = if (isEmailValid) Color.Gray else Color.Red,
                            shape = RoundedCornerShape(4.dp)
                        ),
                    singleLine = true,
                    placeholder = { Text(text = "Email") },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
            }
        }

        if (!isUsernameValid) {
            Text(
                text = "Username should be at least 4 characters long and contain only lowercase letters and numbers.",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(start = 8.dp)
            )
        }

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                isUsernameValid = authViewModel.isUsernameValid(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            singleLine = true,
            placeholder = { Text(text = "Username") },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            isError = !isUsernameValid,
            visualTransformation = VisualTransformation.None,
        )


        // Password TextField with strength validation
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                // @todo Display the password strength message with color instead of just displaying error
                // This will be improved in future
                if (getPasswordStrengthMessage(password) != "Valid password") {
                    Text(
                        text = passwordStrengthMessage,
                        color = getPasswordStrengthColor(passwordStrengthMessage),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(start = 8.dp)
                    )
                }
                PasswordToggleTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordStrengthMessage = getPasswordStrengthMessage(it)
                    },
                    label = "Password",
                    isPasswordVisible = isPasswordVisible,
                    onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .focusRequester(passwordFocusRequester),
                    focusDirection = FocusDirection.Down,
                    imeAction = ImeAction.Next
                )
            }
        }

        // Confirm Password TextField with matching logic
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                // Display an error message if passwords don't match
                if (!doPasswordsMatch) {
                    Text(
                        text = "Passwords do not match",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(start = 8.dp)
                    )
                }

                PasswordToggleTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        doPasswordsMatch = it == password
                    },
                    label = "Confirm password",
                    isPasswordVisible = isConfirmPasswordVisible,
                    onTogglePasswordVisibility = {
                        isConfirmPasswordVisible = !isConfirmPasswordVisible
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .focusRequester(passwordFocusRequester),
                    focusDirection = FocusDirection.Down,
                    imeAction = ImeAction.Done
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                    if (authViewModel.isPasswordStrong(password)) {
                        if (password == confirmPassword) {
                            // Proceed with Firebase sign-up
                            authViewModel.registerUser(email, password, username) { isSuccess, _ ->
                                if (isSuccess) {
                                    // Registration successful
                                    Log.d(tag, "Registration successful.")
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Registration successful.")
                                    }
                                    navController.navigate(NavigationActions.GoToLogin)
                                } else {
                                    // Registration failed
                                    Log.d(
                                        tag,
                                        "Registration error: Sorry, there was an issue with your registration. Please review your input and ensure it meets the required format and criteria."
                                    )
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Sorry, there was an issue with your registration. Please review your input and ensure it meets the required format and criteria.")
                                    }
                                }
                            }
                        } else {
                            // The password and confirm password do not match.
                            Log.d(
                                tag,
                                "Registration error: The password and confirm password do not match."
                            )
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("The password and confirm password do not match.")
                            }
                        }

                    } else {
                        // Password doesn't meet strength requirements
                        Log.d(
                            tag,
                            "Registration error: Password doesn't meet strength requirements."
                        )
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Password doesn't meet strength requirements.")
                        }
                    }
                } else {
                    // Empty input
                    Log.d(tag, "Email, username and password cannot be empty.")
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Email, username and password cannot be empty.")
                    }
                }

            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Sign Up")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { navController.navigate(NavigationActions.GoToLogin) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Already have an account? Log in")
        }
    }

    AppSnackbar(
        snackbarHostState = snackbarHostState,
        modifier = Modifier.padding(16.dp),
    )
}


// Function to check email validity
// @todo extract this
fun isEmailValid(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

fun getPasswordStrengthMessage(password: String): String {
    // Return a message indicating the strength, e.g., "Weak," "Medium," or "Strong"

    val length = password.length
    val hasLowerCase = password.any { it.isLowerCase() }
    val hasUpperCase = password.any { it.isUpperCase() }
    val hasDigit = password.any { it.isDigit() }

    return when {
        length < 8 || !hasLowerCase || !hasUpperCase || !hasDigit -> "Password must be a minimum of 8 characters and include at least one uppercase letter, one lowercase letter, and one digit."
        else -> "Valid password"
    }
}

fun getPasswordStrengthColor(strengthMessage: String): Color {
    // Set the color based on the strength message

    return when (strengthMessage) {
        //@todo implement color strength check later on
        "Password must be a minimum of 8 characters and include at least one uppercase letter, one lowercase letter, and one digit." -> Color.Red
        "Valid password" -> Color.Black
        else -> Color.Black
    }
}


@Preview
@Composable
fun RegistrationScreenPreview() {
    val navController = rememberNavController()
    RegistrationScreen(navController)
}