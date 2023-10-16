package com.rukavina.gymbuddy.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rukavina.gymbuddy.auth.LoginScreen
import com.rukavina.gymbuddy.auth.RegistrationScreen

@Composable
fun NavigationGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = NavigationActions.GoToLogin
    ) {
        composable(NavigationActions.GoToLogin) {
            LoginScreen(navController)
        }
        composable(NavigationActions.GoToRegistration) {
            RegistrationScreen(navController)
        }
    }
}