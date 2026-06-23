package org.archuser.mqttnotify.ui.screen

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.archuser.mqttnotify.domain.model.ConnectionSnapshot
import org.archuser.mqttnotify.domain.model.ConnectionStatus
import org.archuser.mqttnotify.domain.model.InboundMessageRecord
import org.archuser.mqttnotify.ui.theme.warningAccentColor
import org.archuser.mqttnotify.ui.theme.warningSurfaceColor
import org.archuser.mqttnotify.ui.viewmodel.ChannelForm
import org.archuser.mqttnotify.ui.viewmodel.ChannelUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelListScreen(
    state: ChannelUiState,
    brokerCount: Int,
    snapshot: ConnectionSnapshot,
    onAddBroker: () -> Unit,
    onAddChannel: () -> Unit,
    onOpenChannel: (Long) -> Unit,
    onEditChannel: (Long) -> Unit,
    onDeleteChannel: (Long) -> Unit,
    onStartListener: () -> Unit,
    onViewService: () -> Unit
) {
    val formatter = SimpleDateFormat("EEE MMM dd HH:mm:ss", Locale.US)

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ListenerBanner(snapshot, onStartListener, onViewService)
        }

        when {
            brokerCount == 0 -> item {
                EmptyState(
                    title = "No brokers",
                    body = "Add an MQTTS broker before creating channels.",
                    action = "ADD BROKER",
                    onAction = onAddBroker
                )
            }

            state.channels.isEmpty() -> item {
                EmptyState(
                    title = "No channels",
                    body = "Add a channel to listen for MQTT topic filters across your enabled brokers.",
                    action = "ADD CHANNEL",
                    onAction = onAddChannel
                )
            }

            else -> items(state.channels, key = { it.id }) { channel ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onOpenChannel(channel.id) },
                            onLongClick = { onEditChannel(channel.id) }
                        ),
                    elevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(">", style = MaterialTheme.typography.h6)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(channel.displayName, style = MaterialTheme.typography.subtitle1)
                                Text(
                                    if (channel.notifyEnabled) "Notifications on" else "Notifications off",
                                    style = MaterialTheme.typography.caption
                                )
                            }
                            Text(
                                text = channel.topicFilter,
                                style = MaterialTheme.typography.body2,
                                fontFamily = FontFamily.Monospace
                            )
                            val latestMessage = state.messages.firstOrNull { it.topic == channel.topicFilter }
                            Text(
                                text = latestMessage?.let { message ->
                                    "${formatter.format(Date(message.receivedAt))}: ${message.payloadPreview.ifBlank { "<empty payload>" }}"
                                } ?: "No messages yet",
                                style = MaterialTheme.typography.caption
                            )
                        }
                        OutlinedButton(onClick = { onDeleteChannel(channel.id) }) {
                            Text("DELETE")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelDetailScreen(
    state: ChannelUiState,
    onClearHistory: () -> Unit,
    onMarkAllRead: () -> Unit,
    onDeleteMessage: (Long) -> Unit
) {
    val channel = state.editing
    val formatter = SimpleDateFormat("EEE MMM dd HH:mm:ss", Locale.US)

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(channel?.displayName ?: "Channel", style = MaterialTheme.typography.h6)
                Text(channel?.topicFilter ?: "", fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onMarkAllRead, enabled = state.messages.any { it.isUnread }) {
                        Text("MARK READ")
                    }
                    OutlinedButton(onClick = onClearHistory) {
                        Text("CLEAR HISTORY")
                    }
                }
                Divider()
            }
        }

        if (state.messages.isEmpty()) {
            item {
                Text("No messages yet")
            }
        }

        items(state.messages, key = { it.id }) { message ->
            MessageRow(message = message, timestamp = formatter.format(Date(message.receivedAt)), onDelete = onDeleteMessage)
        }
    }
}

@Composable
fun ChannelEditScreen(
    state: ChannelUiState,
    onChange: (ChannelForm) -> Unit,
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
    val form = state.form

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = form.name,
            onValueChange = { onChange(form.copy(name = it)) },
            label = { Text("Channel name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )
        OutlinedTextField(
            value = form.topicFilter,
            onValueChange = { onChange(form.copy(topicFilter = it)) },
            label = { Text("Topic filter") },
            placeholder = { Text("home/+/temperature") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = doneKeyboardOptions,
            keyboardActions = doneKeyboardActions
        )
        Text("Examples: william/important, shared/garage, home/+/temperature, alerts/#")

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = form.notifyEnabled, onCheckedChange = { onChange(form.copy(notifyEnabled = it)) })
            Text("Notify for new messages")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = form.hideRetained, onCheckedChange = { onChange(form.copy(hideRetained = it)) })
            Text("Hide retained messages from history")
        }

        state.validationError?.let {
            Text(it, color = MaterialTheme.colors.error)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave) { Text("SAVE") }
            OutlinedButton(onClick = onCancel) { Text("CANCEL") }
        }
    }
}

@Composable
private fun ListenerBanner(snapshot: ConnectionSnapshot, onStart: () -> Unit, onViewService: () -> Unit) {
    when (snapshot.status) {
        ConnectionStatus.CONNECTED -> return
        ConnectionStatus.ERROR -> Card(elevation = 1.dp, backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.08f)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Broker has errors", style = MaterialTheme.typography.subtitle1)
                Text(snapshot.lastError ?: "Connection error")
                Button(onClick = onViewService) { Text("VIEW") }
            }
        }
        else -> Card(elevation = 1.dp, backgroundColor = warningSurfaceColor()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "MQTT listener is stopped",
                    style = MaterialTheme.typography.subtitle1,
                    color = warningAccentColor()
                )
                Text("No brokers are currently connected.")
                Button(onClick = onStart) { Text("START") }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String, action: String, onAction: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 1.dp) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.h6)
            Text(body)
            Button(onClick = onAction) {
                Text(action)
            }
        }
    }
}

@Composable
private fun MessageRow(message: InboundMessageRecord, timestamp: String, onDelete: (Long) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 1.dp) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(timestamp, style = MaterialTheme.typography.caption)
            Text(message.payloadPreview)
            Text(
                "Broker ${message.brokerId} · ${message.topic}",
                style = MaterialTheme.typography.caption,
                fontFamily = FontFamily.Monospace
            )
            if (message.retained) {
                Text("Retained", color = warningAccentColor())
            }
            Spacer(modifier = Modifier.height(2.dp))
            OutlinedButton(onClick = { onDelete(message.id) }) {
                Text("DELETE MESSAGE")
            }
        }
    }
}
