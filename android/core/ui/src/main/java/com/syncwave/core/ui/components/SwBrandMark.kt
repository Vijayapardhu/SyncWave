package com.syncwave.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.syncwave.core.ui.SwColors

/**
 * SyncWave symbol: two offset waves meeting at a dot.
 * Scales to any size — the stroke width is a function of the canvas
 * size so it stays crisp at 24dp or 240dp.
 */
@Composable
fun SwBrandMark(
    modifier: Modifier = Modifier.size(120.dp),
    color: Color = SwColors.Ink,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.06f
        val top = h * 0.30f
        val bot = h * 0.70f
        val midY = h * 0.50f

        val topPath = Path().apply {
            moveTo(0f, top)
            cubicTo(
                w * 0.10f, top - h * 0.18f,
                w * 0.22f, top - h * 0.18f,
                w * 0.33f, top,
            )
            cubicTo(
                w * 0.45f, top + h * 0.18f,
                w * 0.55f, top + h * 0.18f,
                w * 0.66f, top,
            )
            cubicTo(
                w * 0.78f, top - h * 0.18f,
                w * 0.88f, top - h * 0.18f,
                w, top,
            )
        }
        val botPath = Path().apply {
            moveTo(0f, bot)
            cubicTo(
                w * 0.10f, bot - h * 0.18f,
                w * 0.22f, bot - h * 0.18f,
                w * 0.33f, bot,
            )
            cubicTo(
                w * 0.45f, bot + h * 0.18f,
                w * 0.55f, bot + h * 0.18f,
                w * 0.66f, bot,
            )
            cubicTo(
                w * 0.78f, bot - h * 0.18f,
                w * 0.88f, bot - h * 0.18f,
                w, bot,
            )
        }
        drawPath(
            topPath,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            botPath,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        // Sync dot at the right end of the top wave.
        drawCircle(
            color = color,
            radius = stroke * 0.85f,
            center = Offset(w * 0.99f, midY),
        )
    }
}
