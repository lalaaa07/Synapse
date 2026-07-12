package com.example.synapse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.synapse.R

// ── Comfortaa — the existing rounded warm font used for headings ──────────────
// Kept as the display / heading face: it reads as warm and approachable,
// which is exactly right for patients. We increase spacing to feel more refined.
val ComfortaaFontFamily = FontFamily(
    Font(R.font.comfortaa_regular,  FontWeight.Normal),
    Font(R.font.comfortaa_semibold, FontWeight.SemiBold),
    Font(R.font.comfortaa_bold,     FontWeight.Bold)
)

val SynapseTypography = Typography(
    // ── Display — large numeric readouts (BPM, score digits) ────────────────
    displayLarge = TextStyle(
        fontFamily   = FontFamily.Monospace,
        fontWeight   = FontWeight.W600,
        fontSize     = 52.sp,
        letterSpacing = (-0.5).sp,
        color        = TextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily   = FontFamily.Monospace,
        fontWeight   = FontWeight.W600,
        fontSize     = 40.sp,
        letterSpacing = (-0.5).sp,
        color        = TextPrimary
    ),
    // ── Headlines — section titles ───────────────────────────────────────────
    headlineLarge = TextStyle(
        fontFamily   = ComfortaaFontFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 30.sp,
        letterSpacing = (-0.3).sp,
        color        = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily   = ComfortaaFontFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 24.sp,
        letterSpacing = (-0.2).sp,
        color        = TextPrimary
    ),
    headlineSmall = TextStyle(
        fontFamily   = ComfortaaFontFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 20.sp,
        letterSpacing = 0.sp,
        color        = TextPrimary
    ),
    // ── Titles — card headings ───────────────────────────────────────────────
    titleLarge = TextStyle(
        fontFamily   = ComfortaaFontFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 18.sp,
        letterSpacing = 0.1.sp,
        color        = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily   = ComfortaaFontFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 15.sp,
        letterSpacing = 0.1.sp,
        color        = TextPrimary
    ),
    titleSmall = TextStyle(
        fontFamily   = ComfortaaFontFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 13.sp,
        letterSpacing = 0.1.sp,
        color        = TextSecondary
    ),
    // ── Body — prose / data ──────────────────────────────────────────────────
    bodyLarge = TextStyle(
        fontFamily   = FontFamily.SansSerif,
        fontWeight   = FontWeight.Normal,
        fontSize     = 16.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.1.sp,
        color        = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily   = FontFamily.SansSerif,
        fontWeight   = FontWeight.Normal,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.1.sp,
        color        = TextPrimary
    ),
    bodySmall = TextStyle(
        fontFamily   = FontFamily.SansSerif,
        fontWeight   = FontWeight.Normal,
        fontSize     = 12.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.2.sp,
        color        = TextSecondary
    ),
    // ── Labels — badges, status chips ───────────────────────────────────────
    labelLarge = TextStyle(
        fontFamily   = FontFamily.SansSerif,
        fontWeight   = FontWeight.W500,
        fontSize     = 14.sp,
        letterSpacing = 0.5.sp,
        color        = TextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily   = FontFamily.SansSerif,
        fontWeight   = FontWeight.W500,
        fontSize     = 12.sp,
        letterSpacing = 0.5.sp,
        color        = TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily   = FontFamily.SansSerif,
        fontWeight   = FontWeight.W500,
        fontSize     = 10.sp,
        letterSpacing = 0.8.sp,
        color        = TextSecondary
    )
)