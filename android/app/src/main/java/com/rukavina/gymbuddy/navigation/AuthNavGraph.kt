package com.rukavina.gymbuddy.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.rukavina.gymbuddy.ui.auth.LoginScreen
import com.rukavina.gymbuddy.ui.auth.RegistrationScreen
import com.rukavina.gymbuddy.ui.guest.GuestViewModel

fun NavGraphBuilder.authNavGraph(navController: NavHostController) {
    navigation(
        startDestination = NavRoutes.Login,
        route = NavRoutes.AuthGraph
    ) {
        composable(NavRoutes.Login) {
            val guestViewModel: GuestViewModel = hiltViewModel()
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(NavRoutes.Registration)
                },
                onContinueAsGuest = {
                    guestViewModel.continueAsGuest {
                        navController.navigate(NavRoutes.Main) {
                            popUpTo(NavRoutes.Splash) { inclusive = true }
                        }
                    }
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
