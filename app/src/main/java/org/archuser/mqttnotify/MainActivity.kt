package org.archuser.mqttnotify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
            MqttNotifyTheme(themePreference = themePreference) {
                AppNav(appChromeViewModel = appChromeViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
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
