package org.archuser.mqttnotify.ui.screen

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archuser.mqttnotify.domain.model.InboundMessageRecord

@Composable
fun MessageFeedScreen(
    messages: List<InboundMessageRecord>,
    onResetUnread: (String) -> Unit,
    onDeleteMessage: (Long) -> Unit
) {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages, key = { it.id }) { msg ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(msg.topic, style = MaterialTheme.typography.titleSmall)
                        if (msg.retained) {
                            Text("retained", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(formatter.format(Date(msg.receivedAt)))
                    Text(msg.payloadPreview)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("QoS ${msg.qos}")
                        Button(onClick = { onResetUnread(msg.topic) }) {
                            Text("Reset unread")
                        }
                        Button(onClick = { onDeleteMessage(msg.id) }) {
                            Text("Delete message")
                        }
                    }
                }
            }
        }
    }
}
