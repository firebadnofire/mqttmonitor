package org.archuser.mqttnotify.connection

import android.app.Notification
import javax.inject.Provider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.archuser.mqttnotify.core.DispatchersProvider
import org.archuser.mqttnotify.core.TimeProvider
import org.archuser.mqttnotify.data.mqtt.MqttClientAdapter
import org.archuser.mqttnotify.data.mqtt.MqttEvent
import org.archuser.mqttnotify.data.security.CredentialsStore
import org.archuser.mqttnotify.domain.model.AppState
import org.archuser.mqttnotify.domain.model.BrokerConfig
import org.archuser.mqttnotify.domain.model.ConnectionMode
import org.archuser.mqttnotify.domain.model.ConnectionSnapshot
import org.archuser.mqttnotify.domain.model.ConnectionStatus
import org.archuser.mqttnotify.domain.model.InboundMessageRecord
import org.archuser.mqttnotify.domain.model.ProtocolVersion
import org.archuser.mqttnotify.domain.model.ThemePreference
import org.archuser.mqttnotify.domain.model.TopicSubscriptionConfig
import org.archuser.mqttnotify.domain.repo.AppStateRepository
import org.archuser.mqttnotify.domain.repo.BrokerRepository
import org.archuser.mqttnotify.domain.repo.DiagnosticsRepository
import org.archuser.mqttnotify.domain.repo.MessageRepository
import org.archuser.mqttnotify.domain.repo.TopicRepository
import org.archuser.mqttnotify.notifications.NotificationController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionCoordinatorImplTest {

    @Test
    fun `persistent mode reconnects after unexpected disconnect`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val provider = FakeAdapterProvider()
        val coordinator = createCoordinator(
            dispatcher = dispatcher,
            adapterProvider = provider,
            brokers = listOf(testBroker(1, "Home MQTT")),
            topics = listOf(testTopic(1, 1, "alerts/home"))
        )

        coordinator.startPersistent()
        advanceUntilIdle()

        val firstAdapter = provider.created.single()
        assertEquals(1, firstAdapter.connectCount)
        assertEquals(listOf("alerts/home"), firstAdapter.subscriptions)

        firstAdapter.emit(MqttEvent.ConnectionChanged(ConnectionStatus.DISCONNECTED))
        advanceUntilIdle()

        assertEquals(2, provider.created.size)
        assertEquals(1, provider.created.last().connectCount)
        assertEquals(listOf("alerts/home"), provider.created.last().subscriptions)
    }

    @Test
    fun `startup disconnected event does not trigger duplicate reconnect`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val provider = FakeAdapterProvider(emitDisconnectedDuringConnect = true)
        val coordinator = createCoordinator(
            dispatcher = dispatcher,
            adapterProvider = provider,
            brokers = listOf(testBroker(1, "Home MQTT")),
            topics = listOf(testTopic(1, 1, "alerts/home"))
        )

        coordinator.startPersistent()
        advanceUntilIdle()

        assertEquals(1, provider.created.size)
        assertEquals(1, provider.created.single().connectCount)
    }

    @Test
    fun `persistent mode allocates one adapter per broker`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val provider = FakeAdapterProvider()
        val coordinator = createCoordinator(
            dispatcher = dispatcher,
            adapterProvider = provider,
            brokers = listOf(
                testBroker(1, "Home MQTT"),
                testBroker(2, "VPS MQTT")
            ),
            topics = listOf(
                testTopic(1, 1, "alerts/home"),
                testTopic(2, 2, "alerts/vps")
            )
        )

        coordinator.startPersistent()
        advanceUntilIdle()

        assertEquals(2, provider.created.size)
        assertTrue(provider.created.all { it.connectCount == 1 })
        assertEquals(listOf("alerts/home"), provider.created[0].subscriptions)
        assertEquals(listOf("alerts/vps"), provider.created[1].subscriptions)
    }

    private fun createCoordinator(
        dispatcher: StandardTestDispatcher,
        adapterProvider: FakeAdapterProvider,
        brokers: List<BrokerConfig>,
        topics: List<TopicSubscriptionConfig>
    ): ConnectionCoordinatorImpl {
        val dispatchers = object : DispatchersProvider {
            override val io = dispatcher
            override val default = dispatcher
            override val main = dispatcher
        }
        return ConnectionCoordinatorImpl(
            mqttClientAdapterProvider = adapterProvider,
            brokerRepository = object : BrokerRepository {
                override fun observeBrokers(): Flow<List<BrokerConfig>> = flowOf(brokers)
                override suspend fun getBroker(id: Long): BrokerConfig? = brokers.firstOrNull { it.id == id }
                override suspend fun saveBroker(config: BrokerConfig): Result<Long> = Result.success(config.id)
                override suspend fun deleteBroker(id: Long) = Unit
            },
            topicRepository = object : TopicRepository {
                override fun observeChannels(): Flow<List<TopicSubscriptionConfig>> = flowOf(topics)
                override fun observeTopicsForBroker(brokerId: Long): Flow<List<TopicSubscriptionConfig>> =
                    flowOf(topics.filter { it.brokerId == brokerId })
                override suspend fun getChannel(id: Long): TopicSubscriptionConfig? = topics.firstOrNull { it.id == id }
                override suspend fun getEnabledChannels(): List<TopicSubscriptionConfig> = topics.filter { it.enabled }
                override suspend fun getEnabledTopicsForBroker(brokerId: Long): List<TopicSubscriptionConfig> =
                    topics.filter { it.brokerId == brokerId && it.enabled }
                override suspend fun upsertTopic(config: TopicSubscriptionConfig): Long = config.id
                override suspend fun deleteTopic(id: Long) = Unit
            },
            messageRepository = object : MessageRepository {
                override fun observeRecentMessages(): Flow<List<InboundMessageRecord>> = flowOf(emptyList())
                override fun observeMessagesForBroker(brokerId: Long): Flow<List<InboundMessageRecord>> = flowOf(emptyList())
                override fun observeMessagesForTopic(topic: String): Flow<List<InboundMessageRecord>> = flowOf(emptyList())
                override suspend fun ingestMessage(
                    brokerId: Long,
                    event: MqttEvent.MessageReceived
                ): InboundMessageRecord = throw NotImplementedError()
                override suspend fun resetUnreadForTopic(brokerId: Long, topic: String) = Unit
                override suspend fun resetUnreadForTopicAcrossBrokers(topic: String) = Unit
                override suspend fun resetUnreadForBroker(brokerId: Long) = Unit
                override suspend fun markMessageRead(messageId: Long) = Unit
                override suspend fun markMessageUnread(messageId: Long) = Unit
                override suspend fun unreadCountForBroker(brokerId: Long): Int = 0
                override suspend fun deleteMessage(messageId: Long) = Unit
                override suspend fun deleteMessagesForTopic(topic: String) = Unit
            },
            appStateRepository = FakeAppStateRepository(),
            credentialsStore = object : CredentialsStore {
                override suspend fun savePassword(alias: String, password: String) = Unit
                override suspend fun getPassword(alias: String): String? = null
                override suspend fun deletePassword(alias: String) = Unit
            },
            notifications = object : NotificationController {
                override fun createChannels() = Unit
                override fun buildPersistentNotification(snapshot: ConnectionSnapshot): Notification {
                    throw UnsupportedOperationException()
                }
                override fun updatePersistentNotification(snapshot: ConnectionSnapshot) = Unit
                override fun notifyTopicMessage(brokerLabel: String, message: InboundMessageRecord) = Unit
            },
            diagnosticsRepository = object : DiagnosticsRepository {
                override fun observeEvents(): Flow<List<String>> = flowOf(emptyList())
                override suspend fun log(event: String) = Unit
            },
            dispatchers = dispatchers,
            timeProvider = object : TimeProvider {
                override fun nowMillis(): Long = 1_000L
            }
        )
    }

    private class FakeAdapterProvider(
        private val emitDisconnectedDuringConnect: Boolean = false
    ) : Provider<MqttClientAdapter> {
        val created = mutableListOf<FakeMqttClientAdapter>()

        override fun get(): MqttClientAdapter =
            FakeMqttClientAdapter(emitDisconnectedDuringConnect).also(created::add)
    }

    private class FakeMqttClientAdapter(
        private val emitDisconnectedDuringConnect: Boolean
    ) : MqttClientAdapter {
        private val events = MutableSharedFlow<MqttEvent>(extraBufferCapacity = 16)
        val subscriptions = mutableListOf<String>()
        var connectCount = 0
        var disconnectCount = 0

        override suspend fun connect(config: BrokerConfig, password: String?): Result<Unit> {
            connectCount += 1
            if (emitDisconnectedDuringConnect) {
                events.emit(MqttEvent.ConnectionChanged(ConnectionStatus.DISCONNECTED))
                yield()
            }
            return Result.success(Unit)
        }

        override suspend fun disconnect() {
            disconnectCount += 1
        }

        override suspend fun subscribe(topic: String, qos: Int): Result<Unit> {
            subscriptions += topic
            return Result.success(Unit)
        }

        override suspend fun unsubscribe(topic: String): Result<Unit> = Result.success(Unit)

        override fun events(): Flow<MqttEvent> = events.asSharedFlow()

        suspend fun emit(event: MqttEvent) {
            events.emit(event)
        }
    }

    private class FakeAppStateRepository : AppStateRepository {
        private val state = MutableStateFlow(defaultState)

        override fun observeState(): StateFlow<AppState> = state.asStateFlow()

        override suspend fun currentState(): AppState = state.value

        override suspend fun setActiveBroker(id: Long?) {
            state.value = state.value.copy(activeBrokerId = id)
        }

        override suspend fun setConnectionMode(mode: ConnectionMode) {
            state.value = state.value.copy(connectionMode = mode)
        }

        override suspend fun setGlobalMuteUntil(until: Long?) {
            state.value = state.value.copy(globalMuteUntil = until)
        }

        override suspend fun setLastSessionStartedAt(time: Long?) {
            state.value = state.value.copy(lastSessionStartedAt = time)
        }

        override suspend fun setThemePreference(preference: ThemePreference) {
            state.value = state.value.copy(themePreference = preference)
        }

        override suspend fun setBatteryOptimizationPromptCompleted(completed: Boolean) {
            state.value = state.value.copy(batteryOptimizationPromptCompleted = completed)
        }

        override suspend fun setStartListenerOnAppLaunch(enabled: Boolean) {
            state.value = state.value.copy(startListenerOnAppLaunch = enabled)
        }

        override suspend fun setStartListenerOnPhoneUnlock(enabled: Boolean) {
            state.value = state.value.copy(startListenerOnPhoneUnlock = enabled)
        }
    }

    private companion object {
        val defaultState = AppState(
            activeBrokerId = null,
            connectionMode = ConnectionMode.VISIBLE_ONLY,
            globalMuteUntil = null,
            lastSessionStartedAt = null,
            themePreference = ThemePreference.SYSTEM,
            batteryOptimizationPromptCompleted = false,
            startListenerOnAppLaunch = false,
            startListenerOnPhoneUnlock = false
        )

        fun testBroker(id: Long, label: String) = BrokerConfig(
            id = id,
            label = label,
            host = "example.com",
            port = 8883,
            tls = true,
            protocolVersion = ProtocolVersion.MQTT_5_0,
            username = null,
            credentialsRef = null,
            clientId = null,
            keepaliveSec = 60,
            cleanStart = true,
            sessionExpirySec = 0,
            lastTestPassedAt = 1L
        )

        fun testTopic(id: Long, brokerId: Long, topicFilter: String) = TopicSubscriptionConfig(
            id = id,
            brokerId = brokerId,
            displayName = topicFilter,
            topicFilter = topicFilter,
            qos = 1,
            enabled = true,
            notifyEnabled = true,
            retainedAsNew = false,
            hideRetained = false
        )
    }
}
