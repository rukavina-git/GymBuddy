package com.rukavina.gymbuddy.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue
import com.rukavina.gymbuddy.ui.frame.EdgeSwipeBack
import com.rukavina.gymbuddy.ui.MainScreen
import com.rukavina.gymbuddy.ui.SplashScreen
import com.rukavina.gymbuddy.ui.theme.sharedAxisIn
import com.rukavina.gymbuddy.ui.theme.sharedAxisOut

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    // F5: onboarding and auth screens; Main has its own edge swipe inside.
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    EdgeSwipeBack(enabled = route != null && route != NavRoutes.Splash && route != NavRoutes.Main) {
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
}
