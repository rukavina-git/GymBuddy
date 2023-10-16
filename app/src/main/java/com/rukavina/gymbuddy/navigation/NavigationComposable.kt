package com.rukavina.gymbuddy.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController

@Composable
fun NavigationComposable() {
    val navController = rememberNavController()
    NavigationGraph(navController)
}