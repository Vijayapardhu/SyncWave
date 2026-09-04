package com.syncwave.core.ui.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.syncwave.core.ui.SwColors
import com.syncwave.core.ui.SwType

/**
 * Monochrome status pill. Active = inverted (black fill, paper ink, paper
 * dot). Inactive = paper fill, ink border, ink dot. No color.
 */
@Composable
fun SwStatusPill(
    label: String,
    active: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val bg = if (active) SwColors.Ink else SwColors.Paper
    val fg = if (active) SwColors.Paper else SwColors.Ink
    val border = if (active) SwColors.Ink else SwColors.Ink
    val dot = if (active) SwColors.Paper else SwColors.Ink

    Row(
        modifier = modifier
            .background(bg, shape = RoundedCornerShape(2.dp))
            .border(1.dp, border, RoundedCornerShape(2.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dot)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label.uppercase(),
            color = fg,
            style = SwType.label.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
            letterSpacing = 1.sp,
        )
    }
}
