package com.rukavina.gymbuddy.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rukavina.gymbuddy.ui.MainScreen
import com.rukavina.gymbuddy.ui.SplashScreen
import com.rukavina.gymbuddy.ui.theme.sharedAxisIn
import com.rukavina.gymbuddy.ui.theme.sharedAxisOut

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Splash,
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        enterTransition = { sharedAxisIn(forward = true) },
        exitTransition = { sharedAxisOut(forward = true) },
        popEnterTransition = { sharedAxisIn(forward = false) },
        popExitTransition = { sharedAxisOut(forward = false) },
    ) {
        composable(NavRoutes.Splash) { SplashScreen(navController) }

        startNavGraph(navController)

        composable(NavRoutes.Main) {
            MainScreen(navController)
        }
    }
}
