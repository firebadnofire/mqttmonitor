package org.archuser.mqttnotify.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.archuser.mqttnotify.ui.viewmodel.BrokerListUiState

@Composable
fun BrokerListScreen(
    state: BrokerListUiState,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onAdd) { Text("ADD BROKER") }
            }
        }

        if (state.brokers.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), elevation = 1.dp) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No brokers", style = MaterialTheme.typography.h6)
                        Text("Add an MQTTS broker before creating channels.")
                        Button(onClick = onAdd) { Text("ADD BROKER") }
                    }
                }
            }
        }

        items(state.brokers, key = { it.id }) { broker ->
            Card(modifier = Modifier.fillMaxWidth(), elevation = 1.dp) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(broker.label, style = MaterialTheme.typography.subtitle1)
                        Text(if (broker.lastTestPassedAt == null) "Not tested" else "Tested")
                    }
                    Text("${broker.host}:${broker.port}", fontFamily = FontFamily.Monospace)
                    Text("TLS: ${if (broker.tls) "Required" else "Disabled"}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onEdit(broker.id) }) { Text("EDIT") }
                        OutlinedButton(onClick = { onDelete(broker.id) }) { Text("DELETE") }
                    }
                }
            }
        }
    }
}
