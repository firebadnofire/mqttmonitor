package org.archuser.mqttnotify.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.archuser.mqttnotify.domain.repo.DiagnosticsRepository
import org.archuser.mqttnotify.domain.repo.AppStateRepository
import org.archuser.mqttnotify.service.PersistentConnectionService

@AndroidEntryPoint
class PersistentModeBootReceiver : BroadcastReceiver() {

    @Inject lateinit var appStateRepository: AppStateRepository
    @Inject lateinit var diagnosticsRepository: DiagnosticsRepository

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_USER_UNLOCKED) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val state = appStateRepository.currentState()
                if (state.startListenerOnPhoneUnlock) {
                    runCatching {
                        PersistentConnectionService.start(context.applicationContext)
                    }.onFailure { error ->
                        diagnosticsRepository.log("Phone unlock listener start failed: ${error.message}")
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
