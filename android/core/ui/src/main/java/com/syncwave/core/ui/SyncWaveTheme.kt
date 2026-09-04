package com.syncwave.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.unit.dp

// All surfaces are ink-black or paper-white. Material3 components that
// default to purple or surface tints will be overridden to these two tones
// so the entire app reads as a single monochrome plate.
private val SwColorScheme = darkColorScheme(
    primary = SwColors.Ink,
    onPrimary = SwColors.Paper,
    secondary = SwColors.Ink,
    onSecondary = SwColors.Paper,
    tertiary = SwColors.Ink,
    onTertiary = SwColors.Paper,
    background = SwColors.Ink,
    onBackground = SwColors.Paper,
    surface = SwColors.Ink,
    onSurface = SwColors.Paper,
    surfaceVariant = SwColors.Coal,
    onSurfaceVariant = SwColors.Slate,
    outline = SwColors.Graphite,
    outlineVariant = SwColors.Slate,
    error = SwColors.Paper,
    onError = SwColors.Ink,
    errorContainer = SwColors.Coal,
)

val SwShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small      = RoundedCornerShape(2.dp),
    medium     = RoundedCornerShape(4.dp),
    large      = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp),
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
