package com.example.synapse

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.synapse.ui.theme.CalmRisk
import com.example.synapse.ui.theme.ModerateRisk
import com.example.synapse.ui.theme.HighRisk
import com.example.synapse.ui.theme.BorderSilver
import com.example.synapse.ui.theme.TextPrimary
import com.example.synapse.ui.theme.TextSecondary

/**
 * Circular arc gauge displaying the 0–10 episode risk score.
 * Track uses warm silver; the progress arc sweeps from emerald → amber → crimson.
 * A soft halo glow appears only at elevated / critical risk.
 */
@Composable
fun RiskGauge(score: Int, modifier: Modifier = Modifier) {
    val animatedScore by animateFloatAsState(
        targetValue   = score.toFloat(),
        animationSpec = tween(durationMillis = 700),
        label         = "riskScoreAnim"
    )

    val gaugeColor = when {
        score >= 8 -> HighRisk
        score >= 5 -> ModerateRisk
        else       -> CalmRisk
    }

    Box(
        modifier          = modifier.size(190.dp),
        contentAlignment  = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val strokeWidth  = 18.dp.toPx()
            val sweepAngle   = 270f
            val startAngle   = 135f
            val diameter     = size.minDimension - strokeWidth * 1.2f
            val topLeft      = Offset(
                (size.width  - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

            // ── Background track — warm silver ──────────────────────────────
            drawArc(
                color      = BorderSilver,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // ── Progress arc ────────────────────────────────────────────────
            val progressSweep = sweepAngle * (animatedScore / 10f)
            drawArc(
                color      = gaugeColor,
                startAngle = startAngle,
                sweepAngle = progressSweep,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // ── Halo glow — only at elevated risk ───────────────────────────
            if (score >= 5) {
                val glowAlpha = if (score >= 8) 0.22f else 0.12f
                drawArc(
                    color      = gaugeColor.copy(alpha = glowAlpha),
                    startAngle = startAngle,
                    sweepAngle = progressSweep,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(
                        width = strokeWidth + 18.dp.toPx(),
                        cap   = StrokeCap.Round
                    )
                )
            }
        }

        // ── Center label ────────────────────────────────────────────────────
        Column(horizontalAlignment = CenterHorizontally) {
            Text(
                text  = "$score",
                style = MaterialTheme.typography.displayLarge,
                color = gaugeColor
            )
            Text(
                text  = "/ 10",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}