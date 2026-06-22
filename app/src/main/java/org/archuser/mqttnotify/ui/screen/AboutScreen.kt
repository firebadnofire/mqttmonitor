package org.archuser.mqttnotify.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(elevation = 1.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MQTT Notify", style = MaterialTheme.typography.h6)
                Text("MQTT Notify is a monitoring terminal for technically literate users who operate their own brokers.")
                Text("It does not promise guaranteed background delivery. Active while visible uses UI visibility as consent. Persistent foreground mode uses the ongoing notification as consent.")
                Text("Messages are stored locally per channel. Muting affects notifications only, not message storage.")
            }
        }
    }
}
