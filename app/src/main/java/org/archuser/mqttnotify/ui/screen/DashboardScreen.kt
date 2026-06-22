package org.archuser.mqttnotify.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archuser.mqttnotify.domain.model.ConnectionMode
import org.archuser.mqttnotify.domain.model.ConnectionStatus
import org.archuser.mqttnotify.ui.viewmodel.DashboardUiState

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onSelectBroker: (Long?) -> Unit,
    onSetMode: (ConnectionMode) -> Unit,
    onOpenTopics: (Long) -> Unit,
    onOpenMessages: (Long) -> Unit
) {
    val modeDescription = when (state.mode) {
        ConnectionMode.VISIBLE_ONLY ->
            "Default and recommended. MQTT stays connected only while the app is visible."
        ConnectionMode.PERSISTENT_FOREGROUND ->
            "Optional background mode. MQTT stays connected while the ongoing notification remains active."
    }
    val connectionSummary = when {
        state.activeBrokerId == null -> "No broker selected"
        state.snapshot.status == ConnectionStatus.CONNECTING -> "Connecting to ${state.snapshot.brokerLabel ?: "broker"}"
        state.snapshot.status == ConnectionStatus.CONNECTED -> "Connected to ${state.snapshot.brokerLabel ?: "broker"}"
        state.snapshot.status == ConnectionStatus.ERROR -> state.snapshot.lastError ?: "Connection error"
        state.mode == ConnectionMode.VISIBLE_ONLY -> "Disconnected until the app is on screen"
        else -> "Foreground service enabled, waiting for a broker connection"
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Active Broker", style = MaterialTheme.typography.titleMedium)
        state.brokers.forEach { broker ->
            FilterChip(
                selected = state.activeBrokerId == broker.id,
                onClick = { onSelectBroker(broker.id) },
                label = { Text(broker.label) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Connection Mode", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.mode == ConnectionMode.VISIBLE_ONLY,
                onClick = { onSetMode(ConnectionMode.VISIBLE_ONLY) },
                label = { Text("Visible only") }
            )
            FilterChip(
                selected = state.mode == ConnectionMode.PERSISTENT_FOREGROUND,
                onClick = { onSetMode(ConnectionMode.PERSISTENT_FOREGROUND) },
                label = { Text("Foreground service") }
            )
        }
        Text(modeDescription, style = MaterialTheme.typography.bodySmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Connection")
                Text(connectionSummary)
                Text(
                    "Mode: ${if (state.mode == ConnectionMode.VISIBLE_ONLY) "Active while visible" else "Persistent foreground service"}"
                )
                Text("Status: ${state.snapshot.status}")
                Text("Broker: ${state.snapshot.brokerLabel ?: "None"}")
                Text("Message count: ${state.snapshot.messageCount}")
                Text("Unread (active broker): ${state.unreadCount}")
                if (!state.snapshot.lastError.isNullOrBlank()) {
                    Text("Error: ${state.snapshot.lastError}", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        val activeId = state.activeBrokerId
        if (activeId != null && state.snapshot.status != ConnectionStatus.CONNECTING) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onOpenTopics(activeId) }) {
                    Text("Topics")
                }
                Button(onClick = { onOpenMessages(activeId) }) {
                    Text("Messages")
                }
            }
        }
    }
}
