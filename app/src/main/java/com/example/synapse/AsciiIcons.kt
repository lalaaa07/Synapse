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
 * Minimal ASCII-art glyphs used throughout the app instead of emoji or
 * generic icon packs — matches the monospace "instrument panel" styling
 * used for live sensor readings.
 */
@Composable
fun AsciiGlyph(
    symbol: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.TextUnit = 20.sp
) {
    Text(
        text = symbol,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = size,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

// Nav bar
const val ASCII_DEVICE = "(((•)))"
const val ASCII_MONITOR = "/\\/\\/\\"
const val ASCII_SETTINGS = "{ ⚙ }"

// Status / indicators
const val ASCII_CHECK = "[✓]"
const val ASCII_CROSS = "[×]"
const val ASCII_WARNING = "[!]"
const val ASCII_HEART = "<3"
const val ASCII_RADAR = "))) "
const val ASCII_ALERT = "▲!▲"
const val ASCII_CONNECTED = "●—●"
const val ASCII_DISCONNECTED = "○···○"