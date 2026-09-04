package com.syncwave.feature.receiver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.syncwave.core.ui.SwColors
import com.syncwave.core.ui.SwType
import com.syncwave.core.ui.components.ButtonVariant
import com.syncwave.core.ui.components.GradientPanel
import com.syncwave.core.ui.components.SwButton
import com.syncwave.core.ui.components.SwPanel
import com.syncwave.core.ui.components.SwStatusPill
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun ReceiverScreen(
    roomCode: String,
    onBack: () -> Unit,
    vm: ReceiverViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(roomCode) { vm.join(roomCode) }
    DisposableEffect(Unit) {
        onDispose { vm.leave() }
    }

    val (statusLabel, statusActive) = when (val s = state) {
        is ReceiverState.Joining   -> "🔄 CONNECTING" to false
        is ReceiverState.Connected -> "✅ LIVE" to true
        is ReceiverState.Error     -> "❌ ERROR" to false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                    )
                )
            )
    ) {
        when (val s = state) {
            is ReceiverState.Connected -> s.video?.let { RemoteVideo(it, modifier = Modifier.fillMaxSize()) }
            else -> {}
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Header with Status and Room Code
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0x80000000),
                                Color(0x40000000),
                            )
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    )
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SwStatusPill(label = statusLabel, active = statusActive)
                    Text(
                        text = "📍 $roomCode",
                        color = SwColors.Paper,
                        style = SwType.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (state is ReceiverState.Error) {
                val msg = (state as ReceiverState.Error).message
                GradientPanel(
                    colors = listOf(Color(0xFFFF6B6B), Color(0xFFFF8787)),
                    contentPadding = PaddingValues(20.dp),
                ) {
                    Text(
                        text = "⚠️ ${msg.uppercase()}",
                        color = SwColors.Paper,
                        style = SwType.body,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            ReceiverControls(onLeave = {
                vm.leave()
                onBack()
            })
        }
    }
}

@Composable
private fun RemoteVideo(track: VideoTrack, modifier: Modifier = Modifier) {
    var attached: VideoTrack? by remember { androidx.compose.runtime.mutableStateOf(null) }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                setEnableHardwareScaler(true)
                setMirror(false)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            }
        },
        update = { view ->
            // Detach the previous track (if any) before attaching the new
            // one, so we never leak sinks across recompositions.
            attached?.let { old ->
                if (old !== track) {
                    old.dispose()
                    view.clearImage()
                }
            }
            track.addSink(view)
            attached = track
        },
    )
}

@Composable
private fun ReceiverControls(onLeave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x80000000),
                        Color(0x40000000),
                    )
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // The in-call controls reserved for V0.2 (audio routing, mute)
        // are shown disabled for now so the layout is final.
        SwButton(
            label = "🔇 MUTE",
            onClick = {},
            modifier = Modifier.weight(1f),
            enabled = false,
            variant = ButtonVariant.PRIMARY,
        )
        SwButton(
            label = "🔊 AUDIO",
            onClick = {},
            modifier = Modifier.weight(1f),
            enabled = false,
            variant = ButtonVariant.PRIMARY,
        )
        SwButton(
            label = "👋 LEAVE",
            onClick = onLeave,
            modifier = Modifier.weight(1f),
            variant = ButtonVariant.DANGER,
        )
    }
}
