package com.example.synapse

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Minimal hand-drawn line icons matching the app's design language.
 * Avoids generic Material icons / emoji for a more distinctive feel.
 */

@Composable
fun SignalIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val cx = size.width / 2f
        val baseY = size.height * 0.88f

        // Dot at the base (the device)
        drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(cx, baseY))

        // Available vertical space above the dot, with a small top margin
        val maxRadius = baseY * 0.85f

        // Three concentric signal arcs radiating upward, scaled to fit within canvas
        listOf(0.4f, 0.68f, 0.95f).forEach { fraction ->
            val radius = maxRadius * fraction
            drawArc(
                color = color,
                startAngle = 210f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(cx - radius, baseY - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = stroke
            )
        }
    }
}

@Composable
fun PulseIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val h = size.height
        val w = size.width
        val midY = h / 2f

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, midY)
            lineTo(w * 0.25f, midY)
            lineTo(w * 0.4f, midY - h * 0.35f)
            lineTo(w * 0.55f, midY + h * 0.4f)
            lineTo(w * 0.7f, midY)
            lineTo(w, midY)
        }
        drawPath(path = path, color = color, style = stroke)
    }
}

@Composable
fun SettingsIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.minDimension / 2.4f
        val innerRadius = outerRadius * 0.4f

        // Center circle
        drawCircle(color = color, radius = innerRadius, center = center, style = stroke)

        // Radiating ticks (simplified gear)
        for (i in 0 until 8) {
            val angle = (i * 45f) * (Math.PI / 180f)
            val startR = outerRadius * 0.75f
            val endR = outerRadius
            val start = Offset(
                center.x + (startR * kotlin.math.cos(angle)).toFloat(),
                center.y + (startR * kotlin.math.sin(angle)).toFloat()
            )
            val end = Offset(
                center.x + (endR * kotlin.math.cos(angle)).toFloat(),
                center.y + (endR * kotlin.math.sin(angle)).toFloat()
            )
            drawLine(color = color, start = start, end = end, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

@Composable
fun ProfileIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val cx = size.width / 2f
        val cy = size.height / 2f
        val w = size.width
        val h = size.height

        // Head circle outline
        drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(cx, h * 0.38f), style = stroke)

        // Shoulder arc outline
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - 7.dp.toPx(), h * 0.62f),
            size = androidx.compose.ui.geometry.Size(14.dp.toPx(), 12.dp.toPx()),
            style = stroke
        )
    }
}