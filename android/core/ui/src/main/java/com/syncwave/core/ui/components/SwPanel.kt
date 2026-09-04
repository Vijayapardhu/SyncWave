package com.syncwave.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.syncwave.core.ui.SwColors

/**
 * Flat monochrome panel. No shadow, no gradient — just a black or white
 * surface with a single hairline border. The whole app is a grid of these.
 */
@Composable
fun SwPanel(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(20.dp),
    background: Color = SwColors.Paper,
    borderColor: Color = SwColors.Ink,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    useGradient: Boolean = false,
    isGlassomorphic: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isGlassomorphic) SwColors.Paper else background)
            .border(borderWidth, borderColor, shape)
            .padding(contentPadding)
    ) {
        content()
    }
}

/**
 * Inverted panel: black surface, paper ink, paper border. Used to break up
 * long page sections without introducing color.
 */
@Composable
fun GradientPanel(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(SwColors.Ink, SwColors.Ink),
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(20.dp),
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SwColors.Ink)
            .padding(contentPadding)
    ) {
        content()
    }
}
