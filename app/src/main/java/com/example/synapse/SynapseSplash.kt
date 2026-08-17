package com.example.synapse

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sqrt
import kotlin.random.Random

private data class Neuron(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float
)

@Composable
fun SynapseSplash(onTransitionComplete: () -> Unit) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0A0015), Color(0xFF0D0628), Color(0xFF0A1628))
    )

    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.8f) }
    val splashAlpha = remember { Animatable(1f) }

    val neurons = remember {
        List(18) {
            Neuron(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                vx = (Random.nextFloat() - 0.5f) * 0.002f,
                vy = (Random.nextFloat() - 0.5f) * 0.002f,
                radius = Random.nextFloat() * 8f + 4f
            )
        }
    }

    // ── Timer controls ──
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, animationSpec = tween(1200, easing = LinearOutSlowInEasing))
        scaleAnim.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))

        // ⏱️ ADJUST SCREEN DURATION HERE:
        // 4500 = 4.5 seconds. Increase this number to keep it on screen longer!
        delay(4500)

        splashAlpha.animateTo(0f, animationSpec = tween(800))
        onTransitionComplete()
    }

    // ── Continuous physics simulation loop ──
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { time ->
                tick = time

                // Update coordinates safely on every screen frame
                neurons.forEach { n ->
                    n.x += n.vx
                    n.y += n.vy
                    if (n.x < 0 || n.x > 1) n.vx *= -1
                    if (n.y < 0 || n.y > 1) n.vy *= -1
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 🚨 Crucial fix: Accessing the 'tick' state forces the Canvas
            // to redraw on every frame updates, keeping the dots moving forever!
            val frame = tick

            val w = size.width
            val h = size.height

            // 1. Draw Synaptic Lines (Connections)
            for (i in neurons.indices) {
                val n1 = neurons[i]
                val p1X = n1.x * w
                val p1Y = n1.y * h

                for (j in i + 1 until neurons.size) {
                    val n2 = neurons[j]
                    val p2X = n2.x * w
                    val p2Y = n2.y * h

                    val dx = p1X - p2X
                    val dy = p1Y - p2Y
                    val distance = sqrt(dx * dx + dy * dy)

                    if (distance < 250f) {
                        val connectionAlpha = (1f - (distance / 250f)) * 0.4f * splashAlpha.value
                        drawLine(
                            color = Color(0xFF818CF8).copy(alpha = connectionAlpha),
                            start = Offset(p1X, p1Y),
                            end = Offset(p2X, p2Y),
                            strokeWidth = 2f
                        )
                    }
                }
            }

            // 2. Draw Neuron Nodes
            neurons.forEach { n ->
                val px = n.x * w
                val py = n.y * h

                drawCircle(
                    color = Color(0xFF6366F1).copy(alpha = 0.15f * splashAlpha.value),
                    radius = n.radius * 2.5f,
                    center = Offset(px, py)
                )
                drawCircle(
                    color = Color(0xFF818CF8).copy(alpha = splashAlpha.value),
                    radius = n.radius,
                    center = Offset(px, py)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) {
            Text(
                text = "Synapse",
                color = Color.White.copy(alpha = alphaAnim.value * splashAlpha.value),
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.scale(scaleAnim.value)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "WEARABLE ASSISTIVE SYSTEM",
                color = Color(0xFF6366F1).copy(alpha = alphaAnim.value * splashAlpha.value),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp
            )
        }
    }
}