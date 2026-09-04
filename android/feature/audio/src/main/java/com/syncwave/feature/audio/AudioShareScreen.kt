package com.syncwave.feature.audio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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

@Composable
fun AudioShareScreen(onBack: () -> Unit, vm: AudioHostViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    val micPermissionLauncher = rememberPermLauncher(
        PermContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            // Stay on Ready; the user can switch to System instead, or just go back.
        }
    }

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val mode = (state as? AudioHostState.Ready)?.mode
            ?: (state as? AudioHostState.Sharing)?.mode
            ?: AudioSourceMode.MIC
        if (mode == AudioSourceMode.MIC) {
            // Microphone doesn't need the projection result; sharing starts
            // directly. We still call startSharing so the VM advances state.
            vm.startSharing(android.app.Activity.RESULT_OK, Intent(), mode)
        } else {
            vm.startSharing(result.resultCode, result.data, mode)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Share audio", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        val code = (state as? AudioHostState.Ready)?.code
            ?: (state as? AudioHostState.Sharing)?.code
            ?: "••••••"
        Text(code, fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(8.dp))
        Text(
            when (state) {
                is AudioHostState.Creating -> "Creating room…"
                is AudioHostState.Ready -> "Waiting for guest…"
                is AudioHostState.Sharing -> "Sharing audio…"
                is AudioHostState.Error -> "Error: ${(state as AudioHostState.Error).message}"
            },
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))

        if (state is AudioHostState.Ready) {
            Text("Audio source", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val current = (state as AudioHostState.Ready).mode
            Row(
                modifier = Modifier.fillMaxWidth().selectable(
                    selected = current == AudioSourceMode.MIC,
                    onClick = { vm.setMode(AudioSourceMode.MIC) }
                ).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = current == AudioSourceMode.MIC, onClick = null)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Microphone", fontWeight = FontWeight.Medium)
                    Text(
                        "Capture your voice from the device mic.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().selectable(
                    selected = current == AudioSourceMode.SYSTEM,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            vm.setMode(AudioSourceMode.SYSTEM)
                        }
                    }
                ).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = current == AudioSourceMode.SYSTEM,
                    onClick = null,
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "System audio"
                        else "System audio (Android 10+)",
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Capture whatever the device is playing (music, video).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val ready = state as? AudioHostState.Ready ?: return@Button
                    startShareForegroundService(context)
                    when (ready.mode) {
                        AudioSourceMode.MIC -> {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            vm.startSharing(android.app.Activity.RESULT_OK, Intent(), ready.mode)
                        }
                        AudioSourceMode.SYSTEM -> {
                            projectionLauncher.launch(vm.projectionRequester.createIntent())
                        }
                    }
                }
            ) { Text("Start sharing") }
        }

        if (state is AudioHostState.Sharing) {
            Button(onClick = {
                vm.stopSharing()
                stopShareForegroundService(context)
            }) { Text("Stop sharing") }
        }

        if (state is AudioHostState.Creating || state is AudioHostState.Error) {
            Button(onClick = {
                if (state is AudioHostState.Error) {
                    stopShareForegroundService(context)
                }
                onBack()
            }) { Text("Back") }
        }
    }
}

private fun startShareForegroundService(context: Context) {
    val intent = Intent().apply {
        component = ComponentName(
            context.packageName,
            "com.syncwave.share.ShareForegroundService"
        )
    }
    runCatching { context.startForegroundService(intent) }
}

private fun stopShareForegroundService(context: Context) {
    val intent = Intent().apply {
        component = ComponentName(
            context.packageName,
            "com.syncwave.share.ShareForegroundService"
        )
    }
    runCatching { context.stopService(intent) }
}
