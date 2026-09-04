package com.syncwave.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.syncwave.core.ui.SwColors

/**
 * Modern premium panel with glass morphism effects
 * - Rounded corners (16dp)
 * - Subtle gradient background
 * - Soft shadow for elevation
 * - Optional border for subtle definition
 */
@Composable
fun SwPanel(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(24.dp),
    background: Color = SwColors.Paper,
    borderColor: Color? = null,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    useGradient: Boolean = false,
    isGlassomorphic: Boolean = false,
    content: @Composable () -> Unit,
) {
    val backgroundColor = when {
        isGlassomorphic -> SwColors.GlassLight
        useGradient -> background
        else -> background
    }
    
    val shape = RoundedCornerShape(16.dp)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = shape,
                clip = false,
            )
            .clip(shape)
            .background(backgroundColor)
            .let { mod ->
                if (borderColor != null) {
                    mod.border(borderWidth, borderColor, shape)
                } else {
                    mod
                }
            }
            .padding(contentPadding)
    ) {
        content()
    }
}

/**
 * Gradient panel with two-color gradient background
 */
@Composable
fun GradientPanel(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(SwColors.PrimaryGradientStart, SwColors.PrimaryGradientEnd),
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(24.dp),
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = shape,
                clip = false,
            )
            .clip(shape)
            .background(Brush.linearGradient(colors = colors))
            .padding(contentPadding)
    ) {
        content()
    }
}
