package com.syncwave.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.syncwave.core.ui.SwColors
import com.syncwave.core.ui.SwType

/**
 * Premium gradient button with smooth interactions
 * - Gradient from blue to cyan (primary) or purple to pink (secondary)
 * - Scale animation on press with shadow effect
 * - Rounded corners with elevation
 * - High contrast, bold typography
 */
@Composable
fun SwButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    inverted: Boolean = false,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scaleProgress by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(100),
        label = "buttonScale",
    )
    
    val shadowProgress by animateFloatAsState(
        targetValue = if (pressed) 4f else 12f,
        animationSpec = tween(100),
        label = "buttonShadow",
    )

    val gradient = when {
        inverted -> Brush.linearGradient(
            colors = listOf(SwColors.Ink, SwColors.Ink.copy(alpha = 0.8f)),
        )
        else -> when (variant) {
            ButtonVariant.PRIMARY -> Brush.linearGradient(
                colors = listOf(SwColors.PrimaryGradientStart, SwColors.PrimaryGradientEnd),
            )
            ButtonVariant.SECONDARY -> Brush.linearGradient(
                colors = listOf(SwColors.AccentPurple, SwColors.AccentPink),
            )
            ButtonVariant.SUCCESS -> Brush.linearGradient(
                colors = listOf(SwColors.SuccessInk, Color(0xFF059669)),
            )
            ButtonVariant.DANGER -> Brush.linearGradient(
                colors = listOf(SwColors.DangerInk, Color(0xFFDC2626)),
            )
        }
    }

    val shape = RoundedCornerShape(12.dp)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .shadow(
                elevation = if (enabled) shadowProgress.dp else 2.dp,
                shape = shape,
                clip = false,
            )
            .clip(shape)
            .background(if (enabled) gradient else Brush.linearGradient(
                colors = listOf(SwColors.SurfaceAlt, SwColors.SurfaceAlt)
            ))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .scale(scaleProgress)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (enabled) SwColors.Paper else SwColors.QuietInk,
            style = SwType.body.copy(fontWeight = FontWeight.Black),
            textAlign = TextAlign.Center,
        )
    }
}

enum class ButtonVariant {
    PRIMARY, SECONDARY, SUCCESS, DANGER
}
