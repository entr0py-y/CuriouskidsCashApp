package com.curiouskids.cashapp.core.designsystem.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VaultaColorScheme = darkColorScheme(
    primary = SkyjumpersPrimary,
    secondary = SkyjumpersSecondary,
    background = SkyjumpersBackground,
    surface = SkyjumpersSurface,
    surfaceVariant = VaultaCardBorder,
    onPrimary = VaultaTextPrimary,
    onBackground = VaultaTextPrimary,
    onSurface = VaultaTextPrimary,
    onSurfaceVariant = VaultaTextMuted,
    error = SkyjumpersError
)

@Composable
fun SkyjumpersTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = VaultaColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = VaultaNavyBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
