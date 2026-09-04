package com.syncwave.feature.audio

import android.app.Activity
import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            vm.startSharing(Activity.RESULT_OK, null, AudioSourceMode.MIC)
        }
    }

    val systemProjectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        vm.startSharing(result.resultCode, result.data, AudioSourceMode.SYSTEM)
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
                        is AudioHostState.Sharing  -> "Sharing ${(state as AudioHostState.Sharing).mode.name.lowercase()} audio."
                        is AudioHostState.Error    -> "Error: ${(state as AudioHostState.Error).message}"
                    },
                    color = SwColors.Slate,
                    style = SwType.body,
                )
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state is AudioHostState.Ready) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SwButton(
                            label = "MIC",
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    vm.startSharing(Activity.RESULT_OK, null, AudioSourceMode.MIC)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        SwButton(
                            label = "SYSTEM AUDIO",
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                    systemProjectionLauncher.launch(mgr.createScreenCaptureIntent())
                                } else {
                                    vm.startSharing(Activity.RESULT_OK, null, AudioSourceMode.SYSTEM)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (state is AudioHostState.Sharing) {
                    SwButton(
                        label = "STOP SHARING",
                        onClick = { vm.stopSharing() },
                        variant = ButtonVariant.DANGER,
                    )
                }
                if (state is AudioHostState.Creating || state is AudioHostState.Error) {
                    SwButton(
                        label = "BACK",
                        onClick = onBack,
                        inverted = true,
                    )
                }
            }
        }
    }
}
