package com.syncwave.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.syncwave.core.ui.SwColors
import com.syncwave.core.ui.SwType

/**
 * Flat monochrome button. Inverted variants flip black/white. Disabled is
 * a paper background with a graphite hairline and graphite ink.
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
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(80),
        label = "buttonScale",
    )

    val bg: Color
    val fg: Color
    val border: Color
    when {
        !enabled -> { bg = SwColors.Paper; fg = SwColors.Slate; border = SwColors.Slate }
        inverted -> { bg = SwColors.Paper; fg = SwColors.Ink; border = SwColors.Ink }
        variant == ButtonVariant.DANGER -> { bg = SwColors.Paper; fg = SwColors.Ink; border = SwColors.Ink }
        else -> { bg = SwColors.Ink; fg = SwColors.Paper; border = SwColors.Ink }
    }

    val shape = RoundedCornerShape(2.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
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
            color = fg,
            style = SwType.body.copy(fontWeight = FontWeight.Black),
            textAlign = TextAlign.Center,
        )
    }
}

enum class ButtonVariant { PRIMARY, SECONDARY, SUCCESS, DANGER }
