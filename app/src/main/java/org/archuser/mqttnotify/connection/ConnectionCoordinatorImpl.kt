package org.archuser.mqttnotify.connection

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.archuser.mqttnotify.core.DispatchersProvider
import org.archuser.mqttnotify.core.TimeProvider
import org.archuser.mqttnotify.core.TopicMatcher
import org.archuser.mqttnotify.data.mqtt.MqttClientAdapter
import org.archuser.mqttnotify.data.mqtt.MqttEvent
import org.archuser.mqttnotify.data.security.CredentialsStore
import org.archuser.mqttnotify.domain.model.BrokerConfig
import org.archuser.mqttnotify.domain.model.ConnectionMode
import org.archuser.mqttnotify.domain.model.ConnectionSnapshot
import org.archuser.mqttnotify.domain.model.ConnectionStatus
import org.archuser.mqttnotify.domain.model.TopicSubscriptionConfig
import org.archuser.mqttnotify.domain.repo.AppStateRepository
import org.archuser.mqttnotify.domain.repo.BrokerRepository
import org.archuser.mqttnotify.domain.repo.DiagnosticsRepository
import org.archuser.mqttnotify.domain.repo.MessageRepository
import org.archuser.mqttnotify.domain.repo.TopicRepository
import org.archuser.mqttnotify.notifications.NotificationController

@Singleton
class ConnectionCoordinatorImpl @Inject constructor(
    private val mqttClientAdapterProvider: Provider<MqttClientAdapter>,
    private val brokerRepository: BrokerRepository,
    private val topicRepository: TopicRepository,
    private val messageRepository: MessageRepository,
    private val appStateRepository: AppStateRepository,
    private val credentialsStore: CredentialsStore,
    private val notifications: NotificationController,
    private val diagnosticsRepository: DiagnosticsRepository,
    private val dispatchers: DispatchersProvider,
    private val timeProvider: TimeProvider
) : ConnectionCoordinator {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val reconcileSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    private val lock = Mutex()

    private val _snapshot = MutableStateFlow(
        ConnectionSnapshot(
            status = ConnectionStatus.DISCONNECTED,
            brokerLabel = null,
            connectedSince = null,
            messageCount = 0,
            lastError = null
        )
    )

    override val snapshot: StateFlow<ConnectionSnapshot> = _snapshot.asStateFlow()

    private var uiVisible: Boolean = false
    private var persistentRunning: Boolean = false
    private var knownBrokers: List<BrokerConfig> = emptyList()
    private var activeTopics: List<TopicSubscriptionConfig> = emptyList()
    private val brokerSessions = mutableMapOf<Long, BrokerSession>()

    init {
        scope.launch {
            appStateRepository.observeState().collectLatest {
                reconcileSignal.tryEmit(Unit)
            }
        }

        scope.launch {
            brokerRepository.observeBrokers().collectLatest { brokers ->
                knownBrokers = brokers
                reconcileSignal.tryEmit(Unit)
            }
        }

        scope.launch {
            topicRepository.observeChannels().collectLatest { topics ->
                activeTopics = topics
                reconcileSignal.tryEmit(Unit)
            }
        }

        scope.launch {
            reconcileSignal.collectLatest {
                reconcile()
            }
        }

        scope.launch {
            while (true) {
                delay(RECONCILE_INTERVAL_MS)
                reconcileSignal.tryEmit(Unit)
            }
        }

        reconcileSignal.tryEmit(Unit)
    }

    override suspend fun setMode(mode: ConnectionMode) {
        appStateRepository.setConnectionMode(mode)
        reconcileSignal.tryEmit(Unit)
    }

    override suspend fun setActiveBroker(brokerId: Long?) {
        appStateRepository.setActiveBroker(brokerId)
        reconcileSignal.tryEmit(Unit)
    }

    override suspend fun onUiVisibilityChanged(visible: Boolean) {
        uiVisible = visible
        reconcileSignal.tryEmit(Unit)
    }

    override suspend fun startPersistent() {
        persistentRunning = true
        appStateRepository.setConnectionMode(ConnectionMode.PERSISTENT_FOREGROUND)
        reconcileSignal.tryEmit(Unit)
    }

    override suspend fun stopPersistent() {
        persistentRunning = false
        appStateRepository.setConnectionMode(ConnectionMode.VISIBLE_ONLY)
        reconcileSignal.tryEmit(Unit)
    }

    private suspend fun reconcile() = lock.withLock {
        val state = appStateRepository.currentState()
        val shouldConnect = when (state.connectionMode) {
            ConnectionMode.VISIBLE_ONLY -> uiVisible
            ConnectionMode.PERSISTENT_FOREGROUND -> persistentRunning
        }

        if (!shouldConnect || knownBrokers.isEmpty()) {
            disconnectAll("No active connection target")
            return
        }

        val wantedBrokerIds = knownBrokers.map { it.id }.toSet()
        val staleBrokerIds = brokerSessions.keys - wantedBrokerIds
        staleBrokerIds.forEach { brokerId ->
            disconnectBroker(brokerId, "Broker removed")
        }

        knownBrokers.forEach { broker ->
            val session = brokerSessions[broker.id]
            if (session == null || session.status == ConnectionStatus.ERROR || session.status == ConnectionStatus.DISCONNECTED) {
                disconnectBroker(broker.id, "Reconnecting broker")
                connectToBroker(broker)
            } else {
                session.broker = broker
                syncSubscriptions(session, activeTopics)
            }
        }

        updateSnapshotLocked()
    }

    private suspend fun connectToBroker(broker: BrokerConfig) {
        val adapter = mqttClientAdapterProvider.get()
        val session = BrokerSession(
            broker = broker,
            adapter = adapter,
            eventJob = scope.launch {
                adapter.events().collect { event ->
                    handleBrokerEvent(broker.id, event)
                }
            }
        )

        brokerSessions[broker.id] = session
        session.status = ConnectionStatus.CONNECTING
        updateSnapshotLocked()

        val password = broker.credentialsRef?.alias?.let { credentialsStore.getPassword(it) }
        val result = adapter.connect(broker, password)
        if (result.isFailure) {
            val message = result.exceptionOrNull()?.message ?: "Connection failed"
            session.status = ConnectionStatus.ERROR
            session.lastError = message
            updateSnapshotLocked()
            diagnosticsRepository.log("Connection failed to ${broker.label}: $message")
            return
        }

        val now = timeProvider.nowMillis()
        session.status = ConnectionStatus.CONNECTED
        session.connectedSince = now
        session.lastError = null
        updateSnapshotLocked()
        appStateRepository.setLastSessionStartedAt(now)
        diagnosticsRepository.log("Connected to ${broker.label}")

        syncSubscriptions(session, activeTopics)
    }

    private suspend fun syncSubscriptions(session: BrokerSession, topics: List<TopicSubscriptionConfig>) {
        if (session.status != ConnectionStatus.CONNECTED) {
            return
        }

        val brokerTopics = topics.filter { it.brokerId == session.broker.id && it.enabled }
        val wanted = brokerTopics.map { it.topicFilter }.toSet()
        val toAdd = wanted - session.subscribedTopics
        val toRemove = session.subscribedTopics - wanted

        toRemove.forEach { topic ->
            if (session.adapter.unsubscribe(topic).isSuccess) {
                session.subscribedTopics.remove(topic)
            }
        }
        toAdd.forEach { topic ->
            val qos = brokerTopics.firstOrNull { it.topicFilter == topic }?.qos ?: 1
            if (session.adapter.subscribe(topic, qos).isSuccess) {
                session.subscribedTopics.add(topic)
            }
        }
    }

    private suspend fun handleBrokerEvent(brokerId: Long, event: MqttEvent) {
        when (event) {
            is MqttEvent.ConnectionChanged -> {
                var shouldReconnect = false
                var shouldResubscribe = false
                lock.withLock {
                    val session = brokerSessions[brokerId]
                    if (session != null) {
                        val wasConnected = session.status == ConnectionStatus.CONNECTED || session.connectedSince != null
                        session.status = event.status
                        if (event.status == ConnectionStatus.CONNECTED) {
                            if (session.connectedSince == null) {
                                session.connectedSince = timeProvider.nowMillis()
                            }
                            shouldResubscribe = true
                        }
                        if (event.status == ConnectionStatus.DISCONNECTED) {
                            session.connectedSince = null
                            session.subscribedTopics.clear()
                            shouldReconnect = wasConnected
                        }
                        updateSnapshotLocked()
                    }
                }
                if (shouldResubscribe) {
                    val session = lock.withLock { brokerSessions[brokerId] }
                    if (session != null) {
                        syncSubscriptions(session, activeTopics)
                    }
                }
                if (shouldReconnect) {
                    reconcileSignal.tryEmit(Unit)
                }
            }
            is MqttEvent.MessageReceived -> handleMessage(brokerId, event)
            is MqttEvent.SubscriptionAck -> {
                val label = lock.withLock { brokerSessions[brokerId]?.broker?.label } ?: "broker $brokerId"
                diagnosticsRepository.log("Subscribed on $label: ${event.topic}")
            }
            is MqttEvent.Error -> lock.withLock {
                val session = brokerSessions[brokerId]
                if (session != null) {
                    session.status = ConnectionStatus.ERROR
                    session.lastError = event.message
                    diagnosticsRepository.log("MQTT error on ${session.broker.label}: ${event.message}")
                    updateSnapshotLocked()
                }
            }
        }
    }

    private suspend fun handleMessage(brokerId: Long, event: MqttEvent.MessageReceived) {
        var brokerLabel: String? = null
        var topics: List<TopicSubscriptionConfig> = emptyList()
        lock.withLock {
            val session = brokerSessions[brokerId]
            if (session != null) {
                session.messageCount += 1
                brokerLabel = session.broker.label
                topics = activeTopics
                updateSnapshotLocked()
            }
        }
        val label = brokerLabel ?: return

        val record = messageRepository.ingestMessage(brokerId, event)

        val matchedTopic = topics
            .filter { it.brokerId == brokerId && it.notifyEnabled && TopicMatcher.matches(it.topicFilter, event.topic) }
            .maxByOrNull { it.topicFilter.length }

        val appState = appStateRepository.currentState()
        val muted = appState.globalMuteUntil?.let { it > timeProvider.nowMillis() } ?: false

        if (matchedTopic != null && !muted && record.isNewActivity) {
            notifications.notifyTopicMessage(label, record)
        }
    }

    private suspend fun disconnectAll(reason: String) {
        brokerSessions.keys.toList().forEach { brokerId ->
            disconnectBroker(brokerId, reason)
        }
        updateSnapshotLocked()
    }

    private suspend fun disconnectBroker(brokerId: Long, reason: String) {
        val session = brokerSessions.remove(brokerId) ?: return
        session.eventJob.cancel()
        session.adapter.disconnect()
        diagnosticsRepository.log("Disconnected from ${session.broker.label}: $reason")
    }

    private fun updateSnapshotLocked() {
        val sessions = brokerSessions.values.toList()
        val connected = sessions.filter { it.status == ConnectionStatus.CONNECTED }
        val connecting = sessions.filter { it.status == ConnectionStatus.CONNECTING }
        val errored = sessions.filter { it.status == ConnectionStatus.ERROR }
        val status = when {
            connected.isNotEmpty() -> ConnectionStatus.CONNECTED
            connecting.isNotEmpty() -> ConnectionStatus.CONNECTING
            errored.isNotEmpty() -> ConnectionStatus.ERROR
            else -> ConnectionStatus.DISCONNECTED
        }
        val brokerLabel = when (sessions.size) {
            0 -> null
            1 -> sessions.first().broker.label
            else -> "${connected.size}/${sessions.size} brokers"
        }
        val lastError = errored.firstOrNull()?.let { "${it.broker.label}: ${it.lastError ?: "Connection error"}" }

        _snapshot.value = ConnectionSnapshot(
            status = status,
            brokerLabel = brokerLabel,
            connectedSince = connected.mapNotNull { it.connectedSince }.minOrNull(),
            messageCount = sessions.sumOf { it.messageCount },
            lastError = lastError
        )
    }

    private data class BrokerSession(
        var broker: BrokerConfig,
        val adapter: MqttClientAdapter,
        val eventJob: Job,
        val subscribedTopics: MutableSet<String> = mutableSetOf(),
        var status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
        var connectedSince: Long? = null,
        var messageCount: Long = 0,
        var lastError: String? = null
    )

    private companion object {
        private const val RECONCILE_INTERVAL_MS = 30_000L
    }
}
