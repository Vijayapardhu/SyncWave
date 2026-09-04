package com.syncwave.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 4dp base grid. Generous whitespace is part of the design language;
// the scale jumps so larger elements feel architectural, not incremental.

data class SwSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val s: Dp = 12.dp,
    val m: Dp = 16.dp,
    val l: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
    val gutter: Dp = 24.dp,
    val edge: Dp = 24.dp,
)

val LocalSwSpacing = staticCompositionLocalOf { SwSpacing() }
