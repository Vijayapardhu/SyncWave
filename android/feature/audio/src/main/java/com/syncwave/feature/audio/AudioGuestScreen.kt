package com.syncwave.feature.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AudioGuestScreen(onBack: () -> Unit, vm: AudioGuestViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var code by remember { mutableStateOf("") }

    DisposableEffect(Unit) { onDispose { vm.leave() } }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Listen to audio", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))

        if (state !is AudioGuestState.Listening) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.uppercase().take(8) },
                label = { Text("Room code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (code.isNotBlank()) vm.join(code.trim()) },
                enabled = code.isNotBlank() && state !is AudioGuestState.Joining,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state is AudioGuestState.Joining) "Joining…" else "Join")
            }
        } else {
            Text("Listening to room $code", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = when (val s = state) {
                AudioGuestState.Idle -> "Enter a room code to listen."
                AudioGuestState.Joining -> "Connecting…"
                AudioGuestState.Listening -> "Audio playing."
                is AudioGuestState.Error -> "Error: ${s.message}"
            },
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = { vm.leave(); onBack() }) { Text("Back") }
    }
}
