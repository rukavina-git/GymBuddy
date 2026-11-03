package com.rukavina.gymbuddy.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rukavina.gymbuddy.ui.auth.CreateAccountScreen
import com.rukavina.gymbuddy.ui.auth.CreateAccountViewModel
import com.rukavina.gymbuddy.ui.auth.CreateFrom
import com.rukavina.gymbuddy.ui.auth.SignInScreen
import com.rukavina.gymbuddy.ui.onboarding.OnboardingViewModel
import com.rukavina.gymbuddy.ui.onboarding.SetupScreen
import com.rukavina.gymbuddy.ui.onboarding.WelcomeScreen

/** Setup's saved-state key for the account step's answer (true = created). */
private const val ACCOUNT_RESULT = "account_result"

/**
 * Welcome, setup, sign-in and create account (Figma 114:20520, amended
 * 2026-11-03: account first). A new account runs setup; signing in, or
 * Google finding an existing account, goes straight to Main.
 */
fun NavGraphBuilder.startNavGraph(navController: NavHostController) {
    composable(NavRoutes.Welcome) {
        WelcomeScreen(
            onCreateAccount = { navController.navigate(NavRoutes.createAccount(CreateFrom.WELCOME)) },
            onHaveAccount = { navController.navigate(NavRoutes.signIn()) },
            onContinueWithout = { navController.navigate(NavRoutes.setup()) },
        )
    }

    composable(
        NavRoutes.Setup,
        arguments = listOf(
            navArgument(OnboardingViewModel.ARG_OFFER_ACCOUNT) {
                type = NavType.BoolType
                defaultValue = true
            }
        ),
    ) { entry ->
        val result by entry.savedStateHandle.getStateFlow<Boolean?>(ACCOUNT_RESULT, null).collectAsStateWithLifecycle()
        SetupScreen(
            viewModel = hiltViewModel(),
            onExit = { navController.popBackStack() },
            onDone = { navController.openMain() },
            onOpenAccountStep = { navController.navigate(NavRoutes.createAccount(CreateFrom.SETUP)) },
            accountResult = result,
            onAccountResultUsed = { entry.savedStateHandle[ACCOUNT_RESULT] = null },
        )
    }

    composable(NavRoutes.SignIn) {
        SignInScreen(
            viewModel = hiltViewModel(),
            onBack = { navController.popBackStack() },
            onDone = { navController.openMain() },
            onOpenCreateAccount = { navController.navigate(NavRoutes.createAccount(CreateFrom.SIGN_IN)) },
        )
    }

    composable(
        NavRoutes.CreateAccount,
        arguments = listOf(
            navArgument(CreateAccountViewModel.ARG_FROM) {
                type = NavType.StringType
                defaultValue = CreateFrom.WELCOME.name
            }
        ),
    ) { entry ->
        val from = entry.arguments?.getString(CreateAccountViewModel.ARG_FROM)
            ?.let { runCatching { CreateFrom.valueOf(it) }.getOrNull() } ?: CreateFrom.WELCOME
        CreateAccountScreen(
            viewModel = hiltViewModel(),
            onExit = { navController.popBackStack() },
            onSignIn = {
                // From Sign in, "Sign in" is just back.
                if (from == CreateFrom.SIGN_IN) navController.popBackStack()
                else navController.navigate(NavRoutes.signIn()) { popUpTo(NavRoutes.CreateAccount) { inclusive = true } }
            },
            onCreated = { newAccount -> navController.afterAccount(from, created = true, newAccount = newAccount) },
            onNotNow = { navController.afterAccount(from, created = false, newAccount = false) },
        )
    }
}

/**
 * Where the create flow leads. Setup gets its answer back. A new account
 * from Welcome or Sign in runs setup; an existing one (Google) or one
 * made from inside the app goes to Main. "Not now" from Welcome or Sign
 * in means guest: setup, without asking again at step 5.
 */
private fun NavHostController.afterAccount(from: CreateFrom, created: Boolean, newAccount: Boolean) {
    // Reached from inside the app (session banner → Sign in → Create one): back to Main.
    val insideMain = currentBackStack.value.any { it.destination.route == NavRoutes.Main }
    when {
        // Google found an account that already exists: it has its own
        // profile and plan, so setup must not overwrite them.
        from == CreateFrom.SETUP && created && !newAccount -> openMain()
        from == CreateFrom.SETUP -> {
            previousBackStackEntry?.savedStateHandle?.set(ACCOUNT_RESULT, created)
            popBackStack()
        }
        from == CreateFrom.SETTINGS || insideMain -> if (created) openMain() else popBackStack()
        created && !newAccount -> openMain()
        // "Not now" counts for setup too: no second offer at step 5.
        else -> navigate(NavRoutes.setup(offerAccount = created)) { popUpTo(NavRoutes.Welcome) }
    }
}

/**
 * Main as the only screen left: back from Home leaves the app. Signing in
 * from inside Main (banner, You) goes back to that Main instead, so its
 * tabs and a running workout stay as they were.
 */
fun NavHostController.openMain() {
    if (popBackStack(NavRoutes.Main, inclusive = false)) return
    navigate(NavRoutes.Main) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

/** Welcome as the only screen left: after a sign-out or an erase. */
fun NavHostController.openWelcome() = navigate(NavRoutes.Welcome) {
    popUpTo(graph.id) { inclusive = true }
    launchSingleTop = true
}
