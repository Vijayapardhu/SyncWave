package com.syncwave.feature.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.syncwave.core.ui.SwColors
import com.syncwave.core.ui.SwType
import com.syncwave.core.ui.components.SwButton
import com.syncwave.core.ui.components.SwPanel

@Composable
fun AudioGuestScreen(
    onBack: () -> Unit,
    roomCode: String? = null,
    vm: AudioGuestViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    var code by remember { mutableStateOf(roomCode.orEmpty()) }
    val activeCode = code.ifBlank { roomCode.orEmpty() }

    DisposableEffect(Unit) { onDispose { vm.leave() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SwColors.Ink)
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("AUDIO GUEST", color = SwColors.Paper, style = SwType.label)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "ROOM CODE",
                color = SwColors.Slate,
                style = SwType.label,
                fontSize = 10.sp,
            )
            androidx.compose.foundation.text.BasicTextField(
                value = code,
                onValueChange = { code = it.uppercase().take(8) },
                singleLine = true,
                textStyle = SwType.code.copy(color = SwColors.Paper, fontSize = 32.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(SwColors.Paper),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SwColors.Coal)
                            .padding(20.dp)
                    ) {
                        if (code.isEmpty()) {
                            Text(
                                "------",
                                color = SwColors.Slate,
                                style = SwType.code.copy(fontSize = 32.sp),
                            )
                        }
                        inner()
                    }
                },
            )
            Text(
                text = when (val s = state) {
                    AudioGuestState.Idle -> "Enter a room code to listen."
                    AudioGuestState.Joining -> "Connecting."
                    AudioGuestState.Listening -> "Audio playing."
                    is AudioGuestState.Error -> "Error: ${s.message}"
                },
                color = SwColors.Slate,
                style = SwType.body,
            )
        }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SwButton(
                label = if (state is AudioGuestState.Joining) "JOINING..." else "JOIN",
                onClick = { if (activeCode.isNotBlank()) vm.join(activeCode.trim()) },
                enabled = activeCode.isNotBlank() && state !is AudioGuestState.Joining,
            )
            SwButton(label = "BACK", onClick = { vm.leave(); onBack() }, inverted = true)
        }
    }
}
