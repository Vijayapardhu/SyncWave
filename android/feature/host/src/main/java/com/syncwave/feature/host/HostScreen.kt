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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.syncwave.core.media.ShareForegroundService
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
            .background(SwColors.Ink)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            HostHeader(state = state, onBack = onBack)

            when (val s = state) {
                is HostState.Creating -> EmptyStage(label = "CREATING ROOM…")
                is HostState.Error    -> EmptyStage(label = "ERROR: ${s.message.uppercase()}")
                is HostState.Ready, is HostState.Sharing -> RoomStage(
                    code = (s as? HostState.Ready)?.code
                        ?: (s as HostState.Sharing).code,
                    sharing = s is HostState.Sharing,
                )
            }

            HostActions(
                state = state,
                onStart = {
                    ShareForegroundService.start(context)
                    projectionLauncher.launch(requester.createIntent())
                },
                onStop  = {
                    vm.stopSharing()
                    ShareForegroundService.stop(context)
                },
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
            color = SwColors.Paper,
            style = SwType.label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
        )
        SwStatusPill(
            label = when (state) {
                is HostState.Creating -> "CONNECTING"
                is HostState.Ready    -> "READY"
                is HostState.Sharing  -> "LIVE"
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
            text = if (sharing) "STREAMING" else "ROOM CODE",
            color = SwColors.Slate,
            style = SwType.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
        )

        GradientPanel(
            contentPadding = PaddingValues(24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "Enter this code",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SwColors.Slate,
                )
                Text(
                    text = code,
                    color = SwColors.Paper,
                    style = SwType.code,
                    textAlign = TextAlign.Center,
                    fontSize = 52.sp,
                )
                Text(
                    "or scan the QR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = SwColors.Slate,
                )
            }
        }

        QrPanel(code = code)
    }
}

@Composable
private fun QrPanel(code: String) {
    val bitmap: Bitmap? = remember(code) { runCatching { encodeQrCode(QrPayload.forRoom(code), 384) }.getOrNull() }
    SwPanel(
        contentPadding = PaddingValues(20.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "SCAN WITH PHONE",
                color = SwColors.Ink,
                style = SwType.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
            )
            Box(
                modifier = Modifier
                    .background(SwColors.Paper)
                    .padding(12.dp)
                    .size(220.dp),
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
    SwPanel(
        contentPadding = PaddingValues(32.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                color = SwColors.Ink,
                style = SwType.title,
                fontWeight = FontWeight.Black,
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
                    label = "START SHARING",
                    onClick = onStart,
                    variant = ButtonVariant.PRIMARY,
                )
            is HostState.Sharing ->
                SwButton(
                    label = "STOP SHARING",
                    onClick = onStop,
                    variant = ButtonVariant.DANGER,
                )
            is HostState.Creating ->
                SwButton(label = "BACK", onClick = onBack, enabled = false)
            is HostState.Error ->
                SwButton(label = "BACK", onClick = onBack, variant = ButtonVariant.DANGER)
        }
    }
}
