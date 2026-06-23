package org.archuser.mqttnotify.ui.screen

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.RadioButton
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archuser.mqttnotify.domain.model.BatteryOptimizationState
import org.archuser.mqttnotify.domain.model.ConnectionMode
import org.archuser.mqttnotify.domain.model.ThemePreference
import org.archuser.mqttnotify.ui.theme.warningAccentColor
import org.archuser.mqttnotify.ui.viewmodel.SettingsUiState

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    batteryOptimizationState: BatteryOptimizationState,
    onMuteForMinutes: (Int) -> Unit,
    onClearMute: () -> Unit,
    onThemeChanged: (ThemePreference) -> Unit,
    onKeepHistoryIndefinitelyChanged: (Boolean) -> Unit,
    onPersistentListenerChanged: (Boolean) -> Unit,
    onStartListenerOnAppLaunchChanged: (Boolean) -> Unit,
    onStartListenerOnPhoneUnlockChanged: (Boolean) -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    val formattedMuteUntil = state.muteUntil?.let {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(it))
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsCard("Appearance") {
            ThemeOption("Follow system", ThemePreference.SYSTEM, state.themePreference, onThemeChanged)
            ThemeOption("Light", ThemePreference.LIGHT, state.themePreference, onThemeChanged)
            ThemeOption("Dark", ThemePreference.DARK, state.themePreference, onThemeChanged)
        }

        SettingsCard("Listener") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = state.connectionMode == ConnectionMode.PERSISTENT_FOREGROUND,
                    onCheckedChange = onPersistentListenerChanged
                )
                Text("Persistent listener")
            }
            Text(
                "Keep MQTT Notify running as a foreground service when listening in the background.",
                style = MaterialTheme.typography.caption
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = state.startListenerOnAppLaunch,
                    onCheckedChange = onStartListenerOnAppLaunchChanged
                )
                Text("Start listener on app launch")
            }
            Text(
                "Starts the foreground MQTT notification listener the first time the app is opened after process start.",
                style = MaterialTheme.typography.caption
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = state.startListenerOnPhoneUnlock,
                    onCheckedChange = onStartListenerOnPhoneUnlockChanged
                )
                Text("Start listener on phone unlock")
            }
            Text(
                "Starts the foreground listener the first time the phone is unlocked after a restart.",
                style = MaterialTheme.typography.caption
            )
        }

        SettingsCard("Notifications") {
            Text(
                if (state.muted) "Global mute active until $formattedMuteUntil" else "Global mute is off",
                color = if (state.muted) warningAccentColor() else MaterialTheme.colors.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onMuteForMinutes(15) }) { Text("MUTE 15 MIN") }
                OutlinedButton(onClick = onClearMute) { Text("CLEAR") }
            }
            Text("Mute notifications without stopping message history.", style = MaterialTheme.typography.caption)
        }

        SettingsCard("History") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = state.keepHistoryIndefinitely,
                    onCheckedChange = onKeepHistoryIndefinitelyChanged
                )
                Text("Keep history indefinitely")
            }
            Text(if (state.keepHistoryIndefinitely) "Automatic trimming is off." else "Keep history: 30 days")
            Text("Messages are stored locally inside each channel feed.")
        }

        SettingsCard("Battery") {
            Text(
                "Battery optimization: ${batteryOptimizationLabel(batteryOptimizationState)}",
                color = if (batteryOptimizationState == BatteryOptimizationState.UNRESTRICTED) {
                    MaterialTheme.colors.onSurface
                } else {
                    warningAccentColor()
                }
            )
            Text(
                "Battery exemption is optional. It can improve persistent-listener reliability, but Android may still limit background work.",
                style = MaterialTheme.typography.caption
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenBatterySettings) { Text("REQUEST BATTERY EXEMPTION") }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 1.dp) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.subtitle1)
            content()
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    value: ThemePreference,
    selected: ThemePreference,
    onThemeChanged: (ThemePreference) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected == value, onClick = { onThemeChanged(value) })
        Text(label)
    }
}

private fun batteryOptimizationLabel(state: BatteryOptimizationState): String = when (state) {
    BatteryOptimizationState.UNRESTRICTED -> "Unrestricted"
    BatteryOptimizationState.OPTIMIZED -> "Optimized"
    BatteryOptimizationState.UNKNOWN -> "Unknown"
}
