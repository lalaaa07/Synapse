package com.example.synapse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * A clean, elevated panel that sits on the white background.
 * On a light theme we use a crisp white surface with a very subtle
 * warm-silver hairline border and a light drop shadow to give
 * physicality without looking heavy.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    tint: Color = Color.White,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation   = 4.dp,
                shape       = shape,
                ambientColor  = tint.copy(alpha = 0.08f),
                spotColor     = Color(0xFF1A1A2E).copy(alpha = 0.08f)
            )
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFFF9F8F6)      // very subtle warm cream at bottom
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8E4DF),      // warm silver top edge
                        Color(0xFFEFECE8)       // softer at bottom
                    )
                ),
                shape = shape
            )
    ) {
        content()
    }
}