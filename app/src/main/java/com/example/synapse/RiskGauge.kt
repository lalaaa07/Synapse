package com.example.synapse

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.synapse.ui.theme.SageAccent
import com.example.synapse.ui.theme.AmberRisk
import com.example.synapse.ui.theme.CoralRisk
import com.example.synapse.ui.theme.SurfaceElevatedDark
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.runtime.getValue

/**
 * Circular arc gauge showing the 0-10 episode risk score.
 * Sweeps from sage (calm) through amber (elevated) to coral (high risk),
 * with a soft glow that intensifies as risk rises.
 */
@Composable
fun RiskGauge(score: Int, modifier: Modifier = Modifier) {
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(durationMillis = 600),
        label = "riskScoreAnim"
    )

    val gaugeColor = when {
        score >= 8 -> CoralRisk
        score >= 5 -> AmberRisk
        else -> SageAccent
    }

    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val strokeWidth = 16.dp.toPx()
            val sweepAngle = 270f
            val startAngle = 135f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

            // Background track
            drawArc(
                color = SurfaceElevatedDark,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc
            val progressSweep = sweepAngle * (animatedScore / 10f)
            drawArc(
                color = gaugeColor,
                startAngle = startAngle,
                sweepAngle = progressSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Soft glow at the tip when risk is elevated+
            if (score >= 5) {
                val glowAlpha = if (score >= 8) 0.35f else 0.2f
                drawArc(
                    color = gaugeColor.copy(alpha = glowAlpha),
                    startAngle = startAngle,
                    sweepAngle = progressSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth + 14.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = CenterHorizontally) {
            Text(
                text = "${score}",
                style = MaterialTheme.typography.displayLarge,
                color = gaugeColor
            )
            Text(
                text = "/ 10",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}