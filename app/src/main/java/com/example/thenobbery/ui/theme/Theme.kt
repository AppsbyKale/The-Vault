package com.example.thenobbery.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = VaultWhite,
    secondary = VaultSilver,
    background = VaultBlack,
    surface = VaultSurface,
    onBackground = VaultWhite,
    onSurface = VaultWhite,
    outline = VaultOutline
)

@Composable
fun NobberyVaultTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set System Status Bar to match OLED Black
            window.statusBarColor = VaultBlack.toArgb()
            window.navigationBarColor = VaultBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = VaultShapes, // This applies the 28.dp rounding to Dialogs
        content = content
    )
}
