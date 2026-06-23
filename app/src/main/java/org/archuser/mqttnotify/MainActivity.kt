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
import org.archuser.mqttnotify.domain.repo.AppStateRepository
import org.archuser.mqttnotify.ui.navigation.AppNav
import org.archuser.mqttnotify.ui.theme.MqttNotifyTheme
import org.archuser.mqttnotify.ui.viewmodel.AppChromeViewModel
import org.archuser.mqttnotify.service.PersistentConnectionService

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var connectionCoordinator: ConnectionCoordinator
    @Inject lateinit var appStateRepository: AppStateRepository
    private val appChromeViewModel: AppChromeViewModel by viewModels()
    private var appLaunchAutostartChecked = false

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
                                "Persistent foreground mode works without a special foreground permission. " +
                                    "This request only asks Android to exempt MQTT Notify from battery optimization. " +
                                    "That can improve background reliability, especially with the screen off, but it is optional " +
                                    "and does not guarantee delivery."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = appChromeViewModel::requestIgnoreBatteryOptimizations) {
                                Text("Request battery exemption")
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
        maybeStartListenerOnAppLaunch()
    }

    override fun onStop() {
        lifecycleScope.launch {
            connectionCoordinator.onUiVisibilityChanged(false)
        }
        super.onStop()
    }

    private fun maybeStartListenerOnAppLaunch() {
        if (appLaunchAutostartChecked) return
        appLaunchAutostartChecked = true

        lifecycleScope.launch {
            if (appStateRepository.currentState().startListenerOnAppLaunch) {
                PersistentConnectionService.start(applicationContext)
            }
        }
    }
}
