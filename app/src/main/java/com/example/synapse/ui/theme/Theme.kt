package com.example.synapse.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SynapseColorScheme = darkColorScheme(
    primary = SageAccent,
    secondary = GoldAccent,
    tertiary = AmberRisk,
    error = CoralRisk,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceElevatedDark,
    outline = BorderDark,
    onPrimary = BackgroundDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun SynapseTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = SynapseColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SynapseTypography,
        shapes = SynapseShapes,
        content = content
    )
}