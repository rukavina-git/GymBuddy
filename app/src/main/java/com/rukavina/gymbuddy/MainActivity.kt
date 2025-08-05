package com.rukavina.gymbuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rukavina.gymbuddy.navigation.AppNavHost
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GymBuddyTheme {
                AppNavHost()
            }
        }
    }
}