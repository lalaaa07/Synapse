package com.example.synapse

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import com.example.synapse.ui.theme.RoseQuartz

/**
 * Animated canvas heart that pulses at the patient's actual BPM.
 * Drawn with bezier curves — no emoji, no icon pack dependency.
 */
@Composable
fun HeartbeatPulse(bpm: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")

    // Real beat timing: 60 000 ms / bpm. Clamp to [300, 1500] ms.
    val beatDuration = if (bpm > 0) (60_000 / bpm).coerceIn(300, 1500) else 900

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.20f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = beatDuration / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartbeatScale"
    )

    Canvas(
        modifier = Modifier
            .size(28.dp)
            .scale(scale)
    ) {
        val w = size.width
        val h = size.height

        // A symmetric heart curve using cubic bezier segments
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.85f)                        // bottom tip

            // right lobe
            cubicTo(
                w * 0.95f, h * 0.55f,
                w * 1.05f, h * 0.10f,
                w * 0.5f,  h * 0.28f
            )

            // left lobe
            cubicTo(
                -w * 0.05f, h * 0.10f,
                w * 0.05f, h * 0.55f,
                w * 0.5f,  h * 0.85f
            )

            close()
        }

        drawPath(path = path, color = RoseQuartz, style = Fill)
    }
}