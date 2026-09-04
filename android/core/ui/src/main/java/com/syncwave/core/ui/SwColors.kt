package com.syncwave.core.ui

import androidx.compose.ui.graphics.Color

// Strict monochrome palette. Black and white with a small range of neutral
// grays for hierarchy. Accent colors (success / danger) are reduced to
// inverted black/white pills so the entire app reads as a single ink plate.
object SwColors {
    val Ink = Color(0xFF000000)        // Pure black — primary ink
    val Paper = Color(0xFFFFFFFF)      // Pure white — primary surface
    val Smoke = Color(0xFFF4F4F4)      // Soft neutral — secondary surface
    val Ash = Color(0xFFE5E5E5)        // Hairline / dividers
    val Graphite = Color(0xFF6B6B6B)   // 60% gray — secondary text
    val Slate = Color(0xFF9A9A9A)      // 50% gray — tertiary text
    val Coal = Color(0xFF1A1A1A)       // Near-black — emphasis surface
    val InvertedInk = Color(0xFFFFFFFF)
    val DangerInk = Color(0xFF000000)  // Treat danger as inverted, not red
    val SuccessInk = Color(0xFF000000) // Treat success as inverted, not green
    val Hairline = Ash
    val SubduedInk = Graphite
    val QuietInk = Slate
    val SurfaceAlt = Smoke

    // Legacy names kept for screens that still reference them. All map to
    // the monochrome set above so any residual call sites do not break.
    val PrimaryGradientStart = Ink
    val PrimaryGradientEnd = Ink
    val AccentPurple = Ink
    val AccentPink = Ink
    val AccentOrange = Ink
    val AccentGreen = Ink
    val AccentYellow = Ink
    val WarningInk = Ink
    val GlassLight = Paper
    val GlassDark = Coal
    val SurfaceOverlay = Color(0x0F000000)
}
