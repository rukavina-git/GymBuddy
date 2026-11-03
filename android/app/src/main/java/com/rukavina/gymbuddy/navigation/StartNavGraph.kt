package com.rukavina.gymbuddy.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rukavina.gymbuddy.ui.auth.SignInMode
import com.rukavina.gymbuddy.ui.auth.SignInScreen
import com.rukavina.gymbuddy.ui.auth.SignInViewModel
import com.rukavina.gymbuddy.ui.onboarding.SetupScreen
import com.rukavina.gymbuddy.ui.onboarding.WelcomeScreen

/**
 * Welcome, setup and sign-in (Figma 114:20520, Plan 7 step 04). Setup
 * only follows "Get started" (owner decision 4A); signing in goes
 * straight to Main.
 */
fun NavGraphBuilder.startNavGraph(navController: NavHostController) {
    composable(NavRoutes.Welcome) {
        WelcomeScreen(
            onGetStarted = { navController.navigate(NavRoutes.Setup) },
            onHaveAccount = { navController.navigate(NavRoutes.signIn()) },
        )
    }

    composable(NavRoutes.Setup) {
        SetupScreen(
            viewModel = hiltViewModel(),
            onExit = { navController.popBackStack() },
            onDone = { navController.openMain() },
        )
    }

    composable(
        NavRoutes.SignIn,
        arguments = listOf(
            navArgument(SignInViewModel.ARG_MODE) {
                type = NavType.StringType
                defaultValue = SignInMode.SIGN_IN.name
            }
        ),
    ) {
        SignInScreen(
            viewModel = hiltViewModel(),
            onBack = { navController.popBackStack() },
            onDone = { navController.openMain() },
            onOpenCreateAccount = { navController.navigate(NavRoutes.signIn(SignInMode.CREATE)) },
        )
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
