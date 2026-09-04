package com.syncwave.feature.audio

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult as rememberPermLauncher
import androidx.activity.result.contract.ActivityResultContracts as PermContracts
import com.syncwave.core.media.ShareForegroundService
import com.syncwave.core.ui.SwColors
import com.syncwave.core.ui.SwMono
import com.syncwave.core.ui.SwType
import com.syncwave.core.ui.components.ButtonVariant
import com.syncwave.core.ui.components.SwButton
import com.syncwave.core.ui.components.SwPanel

@Composable
fun AudioShareScreen(onBack: () -> Unit, vm: AudioHostViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    val micPermissionLauncher = rememberPermLauncher(
        PermContracts.RequestPermission()
    ) { granted -> if (!granted) { /* stay on Ready; user can switch to SYSTEM or back */ } }

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val mode = (state as? AudioHostState.Ready)?.mode
            ?: (state as? AudioHostState.Sharing)?.mode
            ?: AudioSourceMode.MIC
        if (mode == AudioSourceMode.MIC) {
            vm.startSharing(android.app.Activity.RESULT_OK, Intent(), mode)
        } else {
            vm.startSharing(result.resultCode, result.data, mode)
        }
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
            Text(
                "AUDIO HOST",
                color = SwColors.Paper,
                style = SwType.label,
            )

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                val code = (state as? AudioHostState.Ready)?.code
                    ?: (state as? AudioHostState.Sharing)?.code
                    ?: "------"
                SwPanel(
                    background = SwColors.Ink,
                    borderColor = SwColors.Paper,
                    contentPadding = PaddingValues(24.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "ROOM CODE",
                            color = SwColors.Slate,
                            style = SwType.label,
                            fontSize = 10.sp,
                        )
                        Text(
                            code,
                            color = SwColors.Paper,
                            style = SwType.code,
                            fontSize = 44.sp,
                            fontFamily = SwMono,
                        )
                    }
                }
                Text(
                    when (state) {
                        is AudioHostState.Creating -> "Creating room."
                        is AudioHostState.Ready    -> "Waiting for guest."
                        is AudioHostState.Sharing  -> "Sharing audio."
                        is AudioHostState.Error    -> "Error: ${(state as AudioHostState.Error).message}"
                    },
                    color = SwColors.Slate,
                    style = SwType.body,
                )
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state is AudioHostState.Ready) {
                    val current = (state as AudioHostState.Ready).mode
                    ModeRow(
                        label = "MICROPHONE",
                        description = "Capture your voice from the device mic.",
                        selected = current == AudioSourceMode.MIC,
                        onSelect = { vm.setMode(AudioSourceMode.MIC) },
                    )
                    ModeRow(
                        label = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            "SYSTEM AUDIO" else "SYSTEM AUDIO (ANDROID 10+)",
                        description = "Capture whatever the device is playing.",
                        selected = current == AudioSourceMode.SYSTEM,
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
                        onSelect = { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) vm.setMode(AudioSourceMode.SYSTEM) },
                    )
                    SwButton(
                        label = "START SHARING",
                        onClick = {
                            val ready = state as? AudioHostState.Ready ?: return@SwButton
                            ShareForegroundService.start(context)
                            when (ready.mode) {
                                AudioSourceMode.MIC -> {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    vm.startSharing(android.app.Activity.RESULT_OK, Intent(), ready.mode)
                                }
                                AudioSourceMode.SYSTEM -> {
                                    projectionLauncher.launch(vm.projectionRequester.createIntent())
                                }
                            }
                        },
                    )
                }
                if (state is AudioHostState.Sharing) {
                    SwButton(
                        label = "STOP SHARING",
                        onClick = {
                            vm.stopSharing()
                            ShareForegroundService.stop(context)
                        },
                        variant = ButtonVariant.DANGER,
                    )
                }
                if (state is AudioHostState.Creating || state is AudioHostState.Error) {
                    SwButton(
                        label = "BACK",
                        onClick = {
                            if (state is AudioHostState.Error) ShareForegroundService.stop(context)
                            onBack()
                        },
                        inverted = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeRow(
    label: String,
    description: String,
    selected: Boolean,
    enabled: Boolean = true,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) SwColors.Paper else SwColors.Coal)
            .padding(16.dp)
            .selectable(selected = selected, enabled = enabled, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(if (selected) SwColors.Ink else SwColors.Slate),
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                label,
                color = if (selected) SwColors.Ink else SwColors.Paper,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
            )
            Text(
                description,
                color = if (selected) SwColors.Ink else SwColors.Slate,
                fontSize = 12.sp,
            )
        }
    }
}
