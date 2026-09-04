package com.syncwave.core.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Two typefaces:
//   - Sans:  Inter / system sans for prose and UI
//   - Mono:  JetBrains Mono / system mono for codes and identifiers
// Weight is the primary lever for hierarchy. We avoid 9pt of gray
// text in favor of bold black at smaller sizes.

val SwSans = FontFamily.SansSerif
val SwMono = FontFamily.Monospace

object SwType {
    val display = TextStyle(
        fontFamily = SwSans, fontWeight = FontWeight.Black,
        fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = (-1.5).sp,
    )
    val hero = TextStyle(
        fontFamily = SwSans, fontWeight = FontWeight.Black,
        fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-1).sp,
    )
    val title = TextStyle(
        fontFamily = SwSans, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp,
    )
    val body = TextStyle(
        fontFamily = SwSans, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 22.sp,
    )
    val label = TextStyle(
        fontFamily = SwSans, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 2.sp,
    )
    val code = TextStyle(
        fontFamily = SwMono, fontWeight = FontWeight.Black,
        fontSize = 64.sp, lineHeight = 68.sp, letterSpacing = 4.sp,
    )
    val mono = TextStyle(
        fontFamily = SwMono, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 18.sp,
    )
}

val SwTypography = Typography(
    displayLarge = SwType.display,
    displayMedium = SwType.hero,
    headlineLarge = SwType.hero,
    headlineMedium = SwType.title,
    titleLarge = SwType.title,
    bodyLarge = SwType.body,
    bodyMedium = SwType.body,
    labelLarge = SwType.label,
)
