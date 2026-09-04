package com.syncwave.feature.host

import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.syncwave.core.ui.SwColors
import com.syncwave.core.ui.SwType
import com.syncwave.core.ui.components.ButtonVariant
import com.syncwave.core.ui.components.GradientPanel
import com.syncwave.core.ui.components.SwButton
import com.syncwave.core.ui.components.SwPanel
import com.syncwave.core.ui.components.SwStatusPill
import com.syncwave.core.ui.qr.QrPayload
import com.syncwave.core.ui.qr.encodeQrCode

@Composable
fun HostScreen(
    onBack: () -> Unit,
    vm: HostViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val requester = remember { vm.projectionRequester }

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        vm.startSharing(result.resultCode, result.data)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        SwColors.Paper,
                        Color(0xFFF0F9FF),
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            HostHeader(state = state, onBack = onBack)

            // Middle: room code or QR
            when (val s = state) {
                is HostState.Creating -> EmptyStage(label = "🔄 CREATING ROOM…")
                is HostState.Error     -> EmptyStage(label = "❌ ${s.message.uppercase()}")
                is HostState.Ready, is HostState.Sharing -> RoomStage(
                    code = (s as? HostState.Ready)?.code
                        ?: (s as HostState.Sharing).code,
                    sharing = s is HostState.Sharing,
                )
            }

            HostActions(
                state = state,
                onStart = { projectionLauncher.launch(requester.createIntent()) },
                onStop  = { vm.stopSharing() },
                onBack  = onBack,
            )
        }
    }
}

@Composable
private fun HostHeader(state: HostState, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "HOST",
            color = SwColors.Ink,
            style = SwType.label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        SwStatusPill(
            label = when (state) {
                is HostState.Creating -> "CONNECTING"
                is HostState.Ready    -> "READY"
                is HostState.Sharing  -> "🔴 LIVE"
                is HostState.Error    -> "ERROR"
            },
            active = state is HostState.Sharing,
        )
    }
}

@Composable
private fun RoomStage(code: String, sharing: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = if (sharing) "🎬 STREAMING LIVE" else "📍 ROOM CODE",
            color = SwColors.SubduedInk,
            style = SwType.label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
        )
        
        // Highlight room code in a colored box
        GradientPanel(
            colors = listOf(
                SwColors.PrimaryGradientStart.copy(alpha = 0.1f),
                SwColors.PrimaryGradientEnd.copy(alpha = 0.05f),
            ),
            contentPadding = PaddingValues(24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Enter this code:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SwColors.SubduedInk,
                )
                Text(
                    text = code,
                    color = SwColors.PrimaryGradientStart,
                    style = SwType.code,
                    textAlign = TextAlign.Center,
                    fontSize = 52.sp,
                )
                Text(
                    "or scan the QR below",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = SwColors.QuietInk,
                )
            }
        }
        
        // QR Code Panel with gradient
        QrPanel(code = code)
    }
}

@Composable
private fun QrPanel(code: String) {
    val bitmap: Bitmap? = remember(code) { runCatching { encodeQrCode(QrPayload.forRoom(code), 384) }.getOrNull() }
    GradientPanel(
        colors = listOf(
            SwColors.Paper,
            SwColors.SurfaceAlt,
        ),
        contentPadding = PaddingValues(24.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "📱 SCAN WITH PHONE",
                color = SwColors.SubduedInk,
                style = SwType.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Box(
                modifier = Modifier
                    .background(SwColors.Paper)
                    .padding(16.dp)
                    .size(240.dp),
                contentAlignment = Alignment.Center,
            ) {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "QR code for room $code",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStage(label: String) {
    GradientPanel(
        colors = listOf(
            Color(0xFFFEE2E2),
            Color(0xFFFFCBCB),
        ),
        contentPadding = PaddingValues(40.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                color = SwColors.DangerInk,
                style = SwType.title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HostActions(
    state: HostState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when (state) {
            is HostState.Ready ->
                SwButton(
                    label = "▶️ START SHARING",
                    onClick = onStart,
                    variant = ButtonVariant.PRIMARY,
                )
            is HostState.Sharing ->
                SwButton(
                    label = "⏹️ STOP SHARING",
                    onClick = onStop,
                    variant = ButtonVariant.DANGER,
                )
            is HostState.Creating ->
                SwButton(label = "BACK", onClick = onBack, enabled = false)
            is HostState.Error ->
                SwButton(label = "↩️ BACK", onClick = onBack, variant = ButtonVariant.DANGER)
        }
    }
}
