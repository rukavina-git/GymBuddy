package com.rukavina.gymbuddy.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Soft Dark. Dark is the only mode in v1 (no light scheme, no dynamic
 * colour). Read design/DESIGN.md before changing anything here.
 */
@Composable
fun GymBuddyTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(
        LocalGymBuddyColors provides GymBuddyColors(),
        LocalGymBuddyType provides GymBuddyType(),
        LocalDimens provides Dimens(),
    ) {
        MaterialTheme(
            colorScheme = SoftDarkColorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

/** Brand extras next to MaterialTheme: colours, custom type, dimens. */
object GymBuddyTheme {
    val colors: GymBuddyColors
        @Composable @ReadOnlyComposable get() = LocalGymBuddyColors.current
    val type: GymBuddyType
        @Composable @ReadOnlyComposable get() = LocalGymBuddyType.current
    val dimens: Dimens
        @Composable @ReadOnlyComposable get() = LocalDimens.current
}
