package com.curiouskids.cashapp.core.designsystem.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SkyjumpersLightColorScheme = lightColorScheme(
    primary = SkyjumpersPrimary,
    secondary = SkyjumpersSecondary,
    background = SkyjumpersBackground,
    surface = SkyjumpersSurface,
    surfaceVariant = SkyjumpersLayerNeutral,
    onPrimary = SkyjumpersSurfaceWhite,
    onBackground = SkyjumpersTextBlack,
    onSurface = SkyjumpersTextBlack,
    onSurfaceVariant = SkyjumpersTextSecondaryGrey,
    error = SkyjumpersError
)

@Composable
fun SkyjumpersTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = SkyjumpersLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SkyjumpersOffWhite.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
