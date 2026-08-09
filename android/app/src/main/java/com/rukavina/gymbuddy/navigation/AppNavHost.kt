package com.rukavina.gymbuddy.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rukavina.gymbuddy.ui.MainScreen
import com.rukavina.gymbuddy.ui.SplashScreen

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Splash
    ) {
        composable(NavRoutes.Splash) { SplashScreen(navController) }

        authNavGraph(navController)

        composable(NavRoutes.Main) {
            MainScreen(navController)
        }
    }
}