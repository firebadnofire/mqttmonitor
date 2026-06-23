package org.archuser.mqttnotify.data.repo

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.archuser.mqttnotify.domain.repo.DiagnosticsRepository

@Singleton
class DiagnosticsRepositoryImpl @Inject constructor() : DiagnosticsRepository {

    private val events = MutableStateFlow<List<String>>(emptyList())
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val zoneId = ZoneId.systemDefault()

    override fun observeEvents(): Flow<List<String>> = events.asStateFlow()

    override suspend fun log(event: String) {
        val timestamped = "${formatter.format(Instant.now().atZone(zoneId))}  $event"
        val next = (events.value + timestamped).takeLast(MAX_EVENTS)
        events.value = next
    }

    private companion object {
        private const val MAX_EVENTS = 300
    }
}
