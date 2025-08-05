package com.rukavina.gymbuddy.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navigation
import com.rukavina.gymbuddy.ui.HomeScreen
import com.rukavina.gymbuddy.ui.ProfileScreen
import com.rukavina.gymbuddy.ui.SessionsScreen
import com.rukavina.gymbuddy.ui.StatisticsScreen
import com.rukavina.gymbuddy.ui.WorkoutsScreen

fun NavGraphBuilder.mainNavGraph(rootNavController: NavHostController) {
    navigation(
        startDestination = NavRoutes.Main,
        route = NavRoutes.Main
    ) {
        composable(NavRoutes.Home) { HomeScreen() }
        composable(NavRoutes.Sessions) { SessionsScreen() }
        composable(NavRoutes.Workouts) { WorkoutsScreen() }
        composable(NavRoutes.Statistics) { StatisticsScreen() }
        composable(NavRoutes.Profile) { ProfileScreen(rootNavController) }
    }
}

@Composable
fun MainScreenShell(navController: NavHostController) {
    val bottomNavItems = BottomNavItem.items
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry.value?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(NavRoutes.Home)
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Home,
            modifier = Modifier.padding(padding)
        ) {
            mainNavGraph(navController)
        }
    }
}