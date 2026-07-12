package com.example.synapse

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import com.example.synapse.ui.theme.HighRisk

@Composable
fun HeartbeatPulse(bpm: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    // Real beat timing: 60000ms / bpm = ms per beat. Fallback to a calm 900ms if bpm is 0.
    val beatDuration = if (bpm > 0) (60000 / bpm).coerceIn(300, 1500) else 900

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = beatDuration / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartbeatScale"
    )

    Text(
        text = "❤️",
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.scale(scale)
    )
}