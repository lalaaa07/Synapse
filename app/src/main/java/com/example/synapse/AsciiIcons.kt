package com.example.synapse

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * Minimal ASCII-style glyphs used throughout the app as status markers.
 * Monospace-rendered so they align cleanly in data rows.
 * No emoji, no icon pack — just clean text symbols.
 */
@Composable
fun AsciiGlyph(
    symbol: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.TextUnit = 16.sp
) {
    Text(
        text       = symbol,
        color      = color,
        fontFamily = FontFamily.Monospace,
        fontSize   = size,
        textAlign  = TextAlign.Center,
        modifier   = modifier
    )
}

// ── Navigation glyphs ──────────────────────────────────────────────────────────
const val ASCII_DEVICE       = "((o))"      // signal / device
const val ASCII_MONITOR      = "_/\\_"       // pulse line
const val ASCII_SETTINGS     = "[--]"       // sliders

// ── Status markers ─────────────────────────────────────────────────────────────
const val ASCII_CHECK        = "[OK]"
const val ASCII_CROSS        = "[--]"
const val ASCII_WARNING      = "[!]"
const val ASCII_HEART        = "<3"
const val ASCII_RADAR        = ")))"
const val ASCII_ALERT        = "/!\\"
const val ASCII_CONNECTED    = "[+]"
const val ASCII_DISCONNECTED = "[ ]"
const val ASCII_PLAYING      = ">>"