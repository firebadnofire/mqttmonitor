package org.archuser.mqttnotify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.archuser.mqttnotify.connection.ConnectionCoordinator
import org.archuser.mqttnotify.ui.navigation.AppNav
import org.archuser.mqttnotify.ui.theme.MqttNotifyTheme
import org.archuser.mqttnotify.ui.viewmodel.AppChromeViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var connectionCoordinator: ConnectionCoordinator
    private val appChromeViewModel: AppChromeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themePreference by appChromeViewModel.themePreference.collectAsStateWithLifecycle()
            val showBatteryPrompt by appChromeViewModel.showBatteryOptimizationPrompt.collectAsStateWithLifecycle()
            MqttNotifyTheme(themePreference = themePreference) {
                AppNav(appChromeViewModel = appChromeViewModel)
                if (showBatteryPrompt) {
                    AlertDialog(
                        onDismissRequest = appChromeViewModel::dismissBatteryOptimizationPrompt,
                        title = { Text("Battery optimization") },
                        text = {
                            Text(
                                "MQTT Notify needs to stay connected in the background to receive broker messages in real time. " +
                                    "Android may stop background apps to save battery. Allowing unrestricted background battery " +
                                    "use improves reliability, especially when the screen is off."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = appChromeViewModel::requestIgnoreBatteryOptimizations) {
                                Text("Allow background listening")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = appChromeViewModel::dismissBatteryOptimizationPrompt) {
                                Text("Skip")
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appChromeViewModel.refreshBatteryOptimizationState()
        lifecycleScope.launch {
            connectionCoordinator.onUiVisibilityChanged(true)
        }
    }

    override fun onStop() {
        lifecycleScope.launch {
            connectionCoordinator.onUiVisibilityChanged(false)
        }
        super.onStop()
    }
}
