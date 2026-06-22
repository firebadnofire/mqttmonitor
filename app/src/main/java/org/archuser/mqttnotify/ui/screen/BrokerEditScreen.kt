package org.archuser.mqttnotify.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.archuser.mqttnotify.ui.viewmodel.BrokerEditUiState

@Composable
fun BrokerEditScreen(
    state: BrokerEditUiState,
    onChange: (BrokerEditUiState) -> Unit,
    onTest: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val doneKeyboardActions = KeyboardActions(
        onDone = {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    )
    val doneKeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = state.label,
            onValueChange = { onChange(state.copy(label = it, tls = true, port = if (state.port.isBlank()) "8883" else state.port)) },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )
        OutlinedTextField(
            value = state.host,
            onValueChange = { onChange(state.copy(host = it, tls = true)) },
            label = { Text("Host") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )
        OutlinedTextField(
            value = state.port.ifBlank { "8883" },
            onValueChange = { onChange(state.copy(port = it, tls = true)) },
            label = { Text("Port") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )
        Text("TLS: Required", fontFamily = FontFamily.Monospace)

        OutlinedTextField(
            value = state.username,
            onValueChange = { onChange(state.copy(username = it, tls = true)) },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = { onChange(state.copy(password = it, tls = true)) },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )

        Text("Advanced", style = MaterialTheme.typography.subtitle1)
        OutlinedTextField(
            value = state.clientId,
            onValueChange = { onChange(state.copy(clientId = it, tls = true)) },
            label = { Text("Client ID") },
            placeholder = { Text("Auto") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )
        OutlinedTextField(
            value = state.keepaliveSec.ifBlank { "60" },
            onValueChange = { onChange(state.copy(keepaliveSec = it, tls = true)) },
            label = { Text("Keepalive seconds") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )
        OutlinedTextField(
            value = state.sessionExpirySec,
            onValueChange = { onChange(state.copy(sessionExpirySec = it, tls = true)) },
            label = { Text("Connection timeout seconds") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )

        state.status?.let {
            Text(
                text = it,
                color = if (it.contains("successful", true)) MaterialTheme.colors.primary else MaterialTheme.colors.error
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave) { Text("SAVE") }
            OutlinedButton(onClick = onTest, enabled = !state.isTesting) {
                Text(if (state.isTesting) "TESTING..." else "TEST CONNECTION")
            }
            OutlinedButton(onClick = onCancel) { Text("CANCEL") }
        }
    }
}
