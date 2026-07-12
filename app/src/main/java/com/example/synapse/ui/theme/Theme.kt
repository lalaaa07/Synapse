package com.example.synapse.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SynapseColorScheme = lightColorScheme(
    primary              = GemSapphire,
    onPrimary            = SurfaceWhite,
    primaryContainer     = GemSapphireLight.copy(alpha = 0.12f),
    onPrimaryContainer   = GemSapphire,
    secondary            = RoseQuartz,
    onSecondary          = SurfaceWhite,
    tertiary             = AmberGem,
    onTertiary           = SurfaceWhite,
    error                = HighRisk,
    onError              = SurfaceWhite,
    background           = WarmWhite,
    onBackground         = TextPrimary,
    surface              = SurfaceWhite,
    onSurface            = TextPrimary,
    surfaceVariant       = SurfaceElevated,
    onSurfaceVariant     = TextSecondary,
    outline              = BorderSilver,
    outlineVariant       = DividerTint,
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
            // Light status bar — dark icons on white background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = SynapseTypography,
        shapes      = SynapseShapes,
        content     = content
    )
}