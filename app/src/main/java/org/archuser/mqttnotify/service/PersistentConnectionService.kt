package org.archuser.mqttnotify.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.archuser.mqttnotify.connection.ConnectionCoordinator
import org.archuser.mqttnotify.domain.repo.DiagnosticsRepository
import org.archuser.mqttnotify.domain.model.ConnectionStatus
import org.archuser.mqttnotify.notifications.NotificationController
import org.archuser.mqttnotify.notifications.NotificationIds

@AndroidEntryPoint
class PersistentConnectionService : Service() {

    @Inject lateinit var coordinator: ConnectionCoordinator
    @Inject lateinit var notificationController: NotificationController
    @Inject lateinit var diagnosticsRepository: DiagnosticsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var snapshotJob: Job? = null
    private var tickerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        notificationController.createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_START -> {
                startForeground(
                    NotificationIds.PERSISTENT_ID,
                    notificationController.buildPersistentNotification(coordinator.snapshot.value)
                )
                serviceScope.launch {
                    coordinator.startPersistent()
                    diagnosticsRepository.log("Persistent service started")
                }
                startNotificationUpdates()
            }

            ACTION_STOP -> {
                serviceScope.launch {
                    coordinator.stopPersistent()
                    diagnosticsRepository.log("Persistent service stopped")
                    stopNotificationUpdates()
                    withContext(Dispatchers.Main.immediate) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }

        return START_STICKY
    }

    private fun startNotificationUpdates() {
        snapshotJob?.cancel()
        tickerJob?.cancel()
        snapshotJob = serviceScope.launch {
            coordinator.snapshot.collectLatest { snapshot ->
                notificationController.updatePersistentNotification(snapshot)
            }
        }
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(NOTIFICATION_REFRESH_MS)
                val snapshot = coordinator.snapshot.value
                if (snapshot.status == ConnectionStatus.CONNECTED) {
                    notificationController.updatePersistentNotification(snapshot)
                }
            }
        }
    }

    private suspend fun stopNotificationUpdates() {
        snapshotJob?.cancelAndJoin()
        snapshotJob = null
        tickerJob?.cancelAndJoin()
        tickerJob = null
    }

    override fun onDestroy() {
        snapshotJob?.cancel()
        tickerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "org.archuser.mqttnotify.action.START_PERSISTENT"
        const val ACTION_STOP = "org.archuser.mqttnotify.action.STOP_PERSISTENT"
        private const val NOTIFICATION_REFRESH_MS = 15_000L

        fun start(context: Context) {
            val intent = Intent(context, PersistentConnectionService::class.java).setAction(ACTION_START)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, PersistentConnectionService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
