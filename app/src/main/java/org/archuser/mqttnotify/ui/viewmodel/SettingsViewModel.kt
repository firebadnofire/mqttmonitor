package org.archuser.mqttnotify.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.archuser.mqttnotify.core.TimeProvider
import org.archuser.mqttnotify.domain.model.ConnectionMode
import org.archuser.mqttnotify.domain.model.RetentionPolicy
import org.archuser.mqttnotify.domain.model.ThemePreference
import org.archuser.mqttnotify.domain.repo.AppStateRepository
import org.archuser.mqttnotify.domain.repo.RetentionRepository

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val retentionRepository: RetentionRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            appStateRepository.observeState().collect { appState ->
                val muted = appState.globalMuteUntil?.let { it > timeProvider.nowMillis() } ?: false
                _state.value = SettingsUiState(
                    muted = muted,
                    muteUntil = appState.globalMuteUntil,
                    themePreference = appState.themePreference,
                    connectionMode = appState.connectionMode,
                    startListenerOnAppLaunch = appState.startListenerOnAppLaunch,
                    startListenerOnPhoneUnlock = appState.startListenerOnPhoneUnlock,
                    keepHistoryIndefinitely = _state.value.keepHistoryIndefinitely
                )
            }
        }

        viewModelScope.launch {
            refreshRetentionState()
        }
    }

    fun clearMute() {
        viewModelScope.launch {
            appStateRepository.setGlobalMuteUntil(null)
        }
    }

    fun muteFor(minutes: Int) {
        viewModelScope.launch {
            val now = timeProvider.nowMillis()
            val currentUntil = appStateRepository.currentState().globalMuteUntil
            val base = currentUntil?.takeIf { it > now } ?: now
            val until = base + minutes.coerceAtLeast(1) * 60_000L
            appStateRepository.setGlobalMuteUntil(until)
        }
    }

    fun setThemePreference(preference: ThemePreference) {
        viewModelScope.launch {
            appStateRepository.setThemePreference(preference)
        }
    }

    fun setStartListenerOnAppLaunch(enabled: Boolean) {
        viewModelScope.launch {
            appStateRepository.setStartListenerOnAppLaunch(enabled)
        }
    }

    fun setStartListenerOnPhoneUnlock(enabled: Boolean) {
        viewModelScope.launch {
            appStateRepository.setStartListenerOnPhoneUnlock(enabled)
        }
    }

    fun setKeepHistoryIndefinitely(enabled: Boolean) {
        viewModelScope.launch {
            val current = retentionRepository.globalDefaultPolicy()
            retentionRepository.upsertPolicy(
                RetentionPolicy(
                    id = current.id,
                    brokerId = null,
                    topicFilter = null,
                    maxMessages = current.maxMessages,
                    maxAgeDays = current.maxAgeDays,
                    trimOnInsert = !enabled
                )
            )
            refreshRetentionState()
        }
    }

    private suspend fun refreshRetentionState() {
        val policy = retentionRepository.globalDefaultPolicy()
        _state.value = _state.value.copy(keepHistoryIndefinitely = !policy.trimOnInsert)
    }
}

data class SettingsUiState(
    val muted: Boolean = false,
    val muteUntil: Long? = null,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val connectionMode: ConnectionMode = ConnectionMode.VISIBLE_ONLY,
    val startListenerOnAppLaunch: Boolean = false,
    val startListenerOnPhoneUnlock: Boolean = false,
    val keepHistoryIndefinitely: Boolean = false
)
