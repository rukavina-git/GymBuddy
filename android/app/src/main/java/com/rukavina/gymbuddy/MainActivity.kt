package com.rukavina.gymbuddy

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.lifecycleScope
import com.rukavina.gymbuddy.update.InAppUpdates
import com.rukavina.gymbuddy.update.UpdateCover
import com.rukavina.gymbuddy.update.UpdateManagers
import com.rukavina.gymbuddy.navigation.AppNavHost
import com.rukavina.gymbuddy.ui.BrandCover
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var updates: InAppUpdates
    private val cover = UpdateCover()
    private var covered by mutableStateOf(cover.covered)
    private val updateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        updates.onResult(it.resultCode)
        cover.onResult(closing = isFinishing)
        covered = cover.covered
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updates = InAppUpdates(this, UpdateManagers.create(this), updateLauncher) { action ->
            cover.onDecided(action)
            covered = cover.covered
        }
        lifecycleScope.launch {
            delay(UpdateCover.TIMEOUT_MS)
            cover.onTimeout()
            covered = cover.covered
        }

        // Edge-to-edge, light bar icons always: the app is dark only
        // (Soft Dark), whatever the system theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            GymBuddyTheme {
                Box {
                    AppNavHost()
                    // Eats taps so nothing under it reacts while covered.
                    if (covered) BrandCover(Modifier.pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updates.check()
    }
}
