package org.archuser.mqttnotify.ui.screen

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archuser.mqttnotify.domain.model.ConnectionMode
import org.archuser.mqttnotify.domain.model.ConnectionSnapshot
import org.archuser.mqttnotify.domain.model.ConnectionStatus

@Composable
fun DiagnosticsScreen(
    snapshot: ConnectionSnapshot,
    events: List<String>,
    mode: ConnectionMode,
    onStartStop: () -> Unit
) {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    val running = snapshot.status == ConnectionStatus.CONNECTED || snapshot.status == ConnectionStatus.CONNECTING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth(), elevation = 1.dp) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Listener", style = MaterialTheme.typography.subtitle1)
                Text("State: ${if (running) "Active" else "Stopped"}")
                Text("Started at: ${snapshot.connectedSince?.let { formatter.format(Date(it)) } ?: "Not running"}")
                Text("Persistent listener: ${if (mode == ConnectionMode.PERSISTENT_FOREGROUND) "Enabled" else "Disabled"}")
                Text("Foreground notification: ${if (mode == ConnectionMode.PERSISTENT_FOREGROUND && running) "Visible" else "Not visible"}")
                Text("Message count: ${snapshot.messageCount}")
                snapshot.lastError?.let { Text("Last error: $it", color = MaterialTheme.colors.error) }
                Button(onClick = onStartStop) {
                    Text(if (running) "STOP LISTENER" else "START LISTENER")
                }
            }
        }

        Text("Recent service events", style = MaterialTheme.typography.subtitle1)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (events.isEmpty()) {
                item { Text("No service events yet") }
            }
            items(events) { item ->
                Text(item)
            }
        }
    }
}
