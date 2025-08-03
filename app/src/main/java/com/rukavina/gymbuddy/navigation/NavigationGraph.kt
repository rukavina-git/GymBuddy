package com.rukavina.gymbuddy.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.rukavina.gymbuddy.auth.LoginScreen
import com.rukavina.gymbuddy.auth.RegistrationScreen
import com.rukavina.gymbuddy.ui.MainScreen

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "auth_graph"
    ) {
        navigation(startDestination = NavigationActions.GoToLogin, route = "auth_graph") {
            composable(NavigationActions.GoToLogin) {
                LoginScreen(navController)
            }
            composable(NavigationActions.GoToRegistration) {
                RegistrationScreen(navController)
            }
        }

        composable(NavigationActions.GoToHome) {
            MainScreen()
        }
    }
}