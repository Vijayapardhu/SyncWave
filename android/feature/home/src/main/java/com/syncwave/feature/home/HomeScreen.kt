package com.syncwave.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syncwave.core.ui.SwColors
import com.syncwave.core.ui.SwType
import com.syncwave.core.ui.components.ButtonVariant
import com.syncwave.core.ui.components.GradientPanel
import com.syncwave.core.ui.components.SwButton

@Composable
fun HomeScreen(onHost: () -> Unit, onJoin: () -> Unit, onAudio: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        SwColors.Paper,
                        Color(0xFFF0F9FF),  // Light blue tint
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Section with Gradient
            GradientPanel(
                colors = listOf(
                    SwColors.PrimaryGradientStart,
                    SwColors.PrimaryGradientEnd,
                ),
                contentPadding = PaddingValues(32.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "SyncWave",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = SwColors.Paper,
                        style = SwType.hero,
                    )
                    Text(
                        "Watch & Listen Together",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = SwColors.Paper.copy(alpha = 0.9f),
                        style = SwType.body,
                    )
                    Text(
                        "Real-time screen and audio sharing\nover peer-to-peer connection",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = SwColors.Paper.copy(alpha = 0.85f),
                        style = SwType.body,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            // Feature Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Share Screen Card
                FeatureCard(
                    title = "Share Screen",
                    description = "Broadcast your screen to viewers",
                    gradient = listOf(Color(0xFF0066FF), Color(0xFF0099CC)),
                    icon = Icons.Default.Share,
                )

                // Share Audio Card
                FeatureCard(
                    title = "Share Audio",
                    description = "Stream system audio seamlessly",
                    gradient = listOf(SwColors.AccentPurple, SwColors.AccentPink),
                    icon = Icons.Default.Phone,
                )

                // Join Room Card
                FeatureCard(
                    title = "Join Room",
                    description = "Connect via room code or QR scan",
                    gradient = listOf(SwColors.SuccessInk, Color(0xFF059669)),
                    icon = Icons.Default.Person,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SwButton(
                    label = "📺 Share Screen",
                    onClick = onHost,
                    variant = ButtonVariant.PRIMARY,
                )
                SwButton(
                    label = "🎤 Share Audio",
                    onClick = onAudio,
                    variant = ButtonVariant.SECONDARY,
                )
                SwButton(
                    label = "👁️ Join Room",
                    onClick = onJoin,
                    variant = ButtonVariant.SUCCESS,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Benefits Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Why SyncWave?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SwColors.Ink,
                    style = SwType.title,
                )
                
                listOf(
                    "✨ Peer-to-peer: No server overhead",
                    "🔒 Secure: Direct connection",
                    "⚡ Fast: Real-time streaming",
                    "🎯 Simple: Room codes or QR scan",
                ).forEach { benefit ->
                    Text(
                        benefit,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = SwColors.SubduedInk,
                        style = SwType.body,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    description: String,
    gradient: List<androidx.compose.ui.graphics.Color>,
    icon: ImageVector,
) {
    GradientPanel(
        colors = gradient,
        contentPadding = PaddingValues(20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SwColors.Paper,
                modifier = Modifier
                    .padding(bottom = 4.dp),
            )
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SwColors.Paper,
                style = SwType.title,
            )
            Text(
                description,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = SwColors.Paper.copy(alpha = 0.9f),
                style = SwType.body,
            )
        }
    }
}
