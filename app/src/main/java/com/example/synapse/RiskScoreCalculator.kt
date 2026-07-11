package com.example.synapse

/**
 * Calculates a 0-10 episode risk score from live telemetry.
 * Pure function — no side effects, easy to unit test independently of BLE/UI.
 */
fun calculateRiskScore(telemetry: Telemetry): Int {
    // Fall detection overrides everything else
    if (telemetry.fall) {
        return 10
    }

    var score = 0

    if (telemetry.agit) score += 3
    if (telemetry.tremor) score += 3

    // BPM: highest matching tier only (not stacked)
    score += when {
        telemetry.bpm > 140 -> 6
        telemetry.bpm > 120 -> 4
        telemetry.bpm > 100 -> 2
        else -> 0
    }

    return score.coerceAtMost(10)
}