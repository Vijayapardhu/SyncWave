package com.syncwave.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SwColorScheme = lightColorScheme(
    primary = SwColors.PrimaryGradientStart,
    onPrimary = SwColors.Paper,
    secondary = SwColors.AccentPurple,
    onSecondary = SwColors.Paper,
    tertiary = SwColors.AccentPink,
    onTertiary = SwColors.Paper,
    background = SwColors.Paper,
    onBackground = SwColors.Ink,
    surface = SwColors.Paper,
    onSurface = SwColors.Ink,
    surfaceVariant = SwColors.SurfaceAlt,
    onSurfaceVariant = SwColors.SubduedInk,
    outline = SwColors.Hairline,
    outlineVariant = SwColors.QuietInk,
    error = SwColors.DangerInk,
    onError = SwColors.Paper,
    errorContainer = Color(0xFFFFEBEE),
)

// Modern rounded shapes for premium look
// Small: buttons and compact elements
// Medium: cards and panels
// Large: full-screen containers
val SwShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // Small accent shapes
    small      = RoundedCornerShape(12.dp),  // Buttons
    medium     = RoundedCornerShape(16.dp),  // Cards and panels
    large      = RoundedCornerShape(20.dp),  // Large containers
    extraLarge = RoundedCornerShape(24.dp),  // Maximum radius
)

@Composable
fun SyncWaveTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSwSpacing provides SwSpacing()) {
        MaterialTheme(
            colorScheme = SwColorScheme,
            typography  = SwTypography,
            shapes      = SwShapes,
            content     = content,
        )
    }
}
