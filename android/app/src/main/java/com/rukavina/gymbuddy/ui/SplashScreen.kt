package com.rukavina.gymbuddy.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.navigation.openMain
import com.rukavina.gymbuddy.navigation.openWelcome
import com.rukavina.gymbuddy.ui.guest.GuestViewModel
import com.rukavina.gymbuddy.ui.theme.AppIcons

/** Same size as Welcome's mark, so the hand-off doesn't jump. */
private val SplashMark = 80.dp

/**
 * App icon on ground while the start route is read: an account's data
 * (even with a dead session) or a guest goes to Main (ADR 0014), anyone
 * else to Welcome. No artificial delay.
 */
@Composable
fun SplashScreen(navController: NavHostController, guestViewModel: GuestViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) {
        if (guestViewModel.opensMain()) navController.openMain() else navController.openWelcome()
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Image(painterResource(AppIcons.Brand.AppIcon), stringResource(R.string.cd_app_name), Modifier.size(SplashMark))
    }
}
