package com.rukavina.gymbuddy.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rukavina.gymbuddy.navigation.BottomNavItem
import com.rukavina.gymbuddy.navigation.NavRoutes


@Composable
fun MainScreen(rootNavController: NavHostController) {
    val bottomNavController = rememberNavController()
    val backStackEntry = bottomNavController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry.value?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                val bottomNavItems = listOf(
                    BottomNavItem.Home,
                    BottomNavItem.Sessions,
                    BottomNavItem.Workouts,
                    BottomNavItem.Statistics,
                    BottomNavItem.Profile
                )
                bottomNavItems.forEach { item ->
                    val selected =
                        currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                // Pop up to the main graph start destination to avoid building a big back stack
                                popUpTo(NavRoutes.Home) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = NavRoutes.Home,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(NavRoutes.Home) { HomeScreen() }
            composable(NavRoutes.Sessions) { SessionsScreen() }
            composable(NavRoutes.Workouts) { WorkoutsScreen() }
            composable(NavRoutes.Statistics) { StatisticsScreen() }
            composable(NavRoutes.Profile) { ProfileScreen(rootNavController) }
        }
    }
}