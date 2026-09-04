package com.syncwave.feature.receiver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.syncwave.core.ui.SwColors
import com.syncwave.core.ui.SwType
import com.syncwave.core.ui.components.ButtonVariant
import com.syncwave.core.ui.components.SwButton
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
    val signal by vm.signalQuality.collectAsState()

    LaunchedEffect(roomCode) { vm.join(roomCode) }
    DisposableEffect(Unit) { onDispose { vm.leave() } }

    val (statusLabel, statusActive) = when (val s = state) {
        is ReceiverState.Idle -> "IDLE" to false
        is ReceiverState.Joining -> "CONNECTING" to false
        is ReceiverState.Signaling -> "CALLING" to false
        is ReceiverState.IceChecking -> "CONNECTING" to false
        is ReceiverState.Connected -> "CONNECTED" to true
        is ReceiverState.Error -> "ERROR" to false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SwColors.Ink)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SwColors.Ink)
                    .border(1.dp, SwColors.Paper, RoundedCornerShape(2.dp))
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SignalIndicator(quality = signal)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = statusLabel,
                            color = SwColors.Paper,
                            style = SwType.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Text(
                        text = roomCode,
                        color = SwColors.Paper,
                        style = SwType.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            if (state is ReceiverState.Error) {
                val msg = (state as ReceiverState.Error).message
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SwColors.Paper, RoundedCornerShape(2.dp))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "ERROR: ${msg.uppercase()}",
                        color = SwColors.Paper,
                        style = SwType.body,
                        fontWeight = FontWeight.Black,
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
private fun SignalIndicator(quality: com.syncwave.feature.receiver.SignalQuality) {
    val count = when (quality) {
        com.syncwave.feature.receiver.SignalQuality.NONE -> 0
        com.syncwave.feature.receiver.SignalQuality.WEAK -> 1
        com.syncwave.feature.receiver.SignalQuality.MEDIUM -> 2
        com.syncwave.feature.receiver.SignalQuality.STRONG -> 3
    }
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { index ->
            val active = index < count
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = (8 + index * 4).dp)
                    .background(
                        color = if (active) SwColors.Paper else SwColors.Slate,
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@Composable
private fun RemoteVideo(track: VideoTrack, modifier: Modifier = Modifier) {
    var attached: VideoTrack? by remember { mutableStateOf(null) }
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
            .background(SwColors.Ink)
            .border(1.dp, SwColors.Paper, RoundedCornerShape(2.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SwButton(
            label = "LEAVE",
            onClick = onLeave,
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.DANGER,
        )
    }
}
