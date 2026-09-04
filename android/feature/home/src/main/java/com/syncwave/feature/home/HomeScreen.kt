package com.syncwave.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syncwave.core.ui.SwColors
import com.syncwave.core.ui.SwType
import com.syncwave.core.ui.components.ButtonVariant
import com.syncwave.core.ui.components.GradientPanel
import com.syncwave.core.ui.components.SwButton
import com.syncwave.core.ui.components.SwPanel

@Composable
fun HomeScreen(onHost: () -> Unit, onJoin: () -> Unit, onAudio: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SwColors.Ink)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Brand mark
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "SYNCWAVE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = SwColors.Paper,
                    style = SwType.label,
                )
                Text(
                    "Share your screen or audio with anyone nearby. No accounts. No installs.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = SwColors.Slate,
                    style = SwType.body,
                )
            }

            // Hero block: inverted panel with product statement
            GradientPanel(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(28.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "ROOM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = SwColors.Slate,
                        style = SwType.label,
                    )
                    Text(
                        "Watch & listen together.",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = SwColors.Paper,
                        style = SwType.hero,
                    )
                    Text(
                        "Peer-to-peer, real-time.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = SwColors.Slate,
                        style = SwType.body,
                    )
                }
            }

            // Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SwButton(
                    label = "SHARE SCREEN",
                    onClick = onHost,
                    variant = ButtonVariant.PRIMARY,
                )
                SwButton(
                    label = "SHARE AUDIO",
                    onClick = onAudio,
                    inverted = true,
                )
                SwButton(
                    label = "JOIN ROOM",
                    onClick = onJoin,
                    inverted = true,
                )
            }

            // Notes panel
            SwPanel(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "NOTES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = SwColors.Ink,
                        style = SwType.label,
                    )
                    listOf(
                        "Direct peer-to-peer connection. No relay.",
                        "Room codes are six characters. QR scan supported.",
                        "Audio-only mode works on Android 10+.",
                    ).forEach { line ->
                        Text(
                            line,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = SwColors.Ink,
                            style = SwType.body,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
