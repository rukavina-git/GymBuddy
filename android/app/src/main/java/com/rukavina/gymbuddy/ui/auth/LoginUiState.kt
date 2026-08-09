package com.rukavina.gymbuddy.ui.auth

/**
 * UI state for the login form fields.
 */
data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false
)

/**
 * UI state for login loading/progress.
 */
data class LoginLoadingState(
    val isLoginLoading: Boolean = false,
    val isGoogleSignInLoading: Boolean = false
)

/**
 * UI state for the account linking dialog.
 */
data class LinkDialogState(
    val isVisible: Boolean = false,
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false
)

/**
 * Combined login screen UI state.
 */
data class LoginUiState(
    val form: LoginFormState = LoginFormState(),
    val loading: LoginLoadingState = LoginLoadingState(),
    val linkDialog: LinkDialogState = LinkDialogState(),
    val showLoginFailedDialog: Boolean = false,
    val errorMessage: String? = null
)
