package com.rukavina.gymbuddy.ui


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rukavina.gymbuddy.ui.guest.GuestViewModel
import kotlinx.coroutines.delay


@Composable
fun SplashScreen(navController: NavHostController, guestViewModel: GuestViewModel = hiltViewModel()) {

    LaunchedEffect(Unit) {
        delay(1000)

        // An account's data (even with its session dead) or a guest (ADR 0014) goes straight in.
        if (guestViewModel.opensMain()) {
            navController.navigate("main") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Loading...", style = MaterialTheme.typography.headlineMedium)
        // @todo: App logo, animation, etc.
    }
}
