package com.rukavina.gymbuddy.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.rukavina.gymbuddy.ui.auth.LoginScreen
import com.rukavina.gymbuddy.ui.auth.RegistrationScreen

fun NavGraphBuilder.authNavGraph(navController: NavHostController) {
    navigation(
        startDestination = NavRoutes.Login,
        route = NavRoutes.AuthGraph
    ) {
        composable(NavRoutes.Login) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(NavRoutes.Registration)
                },
                onLoginSuccess = {
                    navController.navigate(NavRoutes.Main) {
                        popUpTo(NavRoutes.Splash) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.Registration) {
            RegistrationScreen(
                onRegistrationSuccess = {
                    navController.navigate(NavRoutes.Main) {
                        popUpTo(NavRoutes.Splash) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
