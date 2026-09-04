package com.syncwave.core.ui.components

import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.syncwave.core.ui.SwColors
import com.syncwave.core.ui.SwType

@Composable
fun SwStatusPill(
    label: String,
    active: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (active) {
        Brush.linearGradient(
            colors = listOf(
                SwColors.SuccessInk.copy(alpha = 0.2f),
                SwColors.SuccessInk.copy(alpha = 0.1f),
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                SwColors.SurfaceAlt,
                SwColors.Paper,
            )
        )
    }
    
    val dotColor = if (active) SwColors.SuccessInk else SwColors.QuietInk
    val borderColor = if (active) SwColors.SuccessInk else SwColors.Hairline
    
    Row(
        modifier = modifier
            .background(backgroundColor, shape = RoundedCornerShape(20.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        // Pulsing dot indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label.uppercase(),
            color = if (active) SwColors.SuccessInk else SwColors.SubduedInk,
            style = SwType.label.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
            letterSpacing = 0.5.sp,
        )
    }
}
