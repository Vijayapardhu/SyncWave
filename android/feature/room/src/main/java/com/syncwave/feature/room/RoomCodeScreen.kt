package com.syncwave.feature.room

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.syncwave.core.ui.SwColors
import com.syncwave.core.ui.SwMono
import com.syncwave.core.ui.SwType
import com.syncwave.core.ui.components.SwButton
import com.syncwave.core.ui.components.SwPanel

@Composable
fun RoomCodeScreen(
    onCancel: () -> Unit,
    onJoined: (String) -> Unit,
    onScan: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize().background(SwColors.Paper)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("JOIN", color = SwColors.Ink, style = SwType.label)

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("ROOM CODE", color = SwColors.SubduedInk, style = SwType.label)
                Spacer(Modifier.height(12.dp))
                CodeInput(
                    value = code,
                    onValueChange = { code = it.uppercase().take(6) },
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                SwButton(
                    label = if (code.length == 6) "JOIN $code" else "ENTER CODE",
                    onClick = { if (code.length == 6) onJoined(code) },
                    enabled = code.length == 6,
                )
                Spacer(Modifier.height(12.dp))
                SwButton(
                    label = "SCAN QR",
                    onClick = onScan,
                    inverted = true,
                )
                Spacer(Modifier.height(12.dp))
                SwButton(
                    label = "CANCEL",
                    onClick = onCancel,
                    inverted = true,
                )
            }
        }
    }
}

@Composable
private fun CodeInput(value: String, onValueChange: (String) -> Unit) {
    val baseStyle = SwType.code.copy(
        color = SwColors.Ink,
        textAlign = TextAlign.Center,
    )
    SwPanel(contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = baseStyle,
            cursorBrush = SolidColor(SwColors.Ink),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Go,
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "—— ——",
                            color = SwColors.QuietInk,
                            style = baseStyle,
                        )
                    }
                    inner()
                }
            },
        )
    }
}
