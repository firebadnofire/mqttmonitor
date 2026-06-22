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
import org.archuser.mqttnotify.domain.model.ThemePreference
import org.archuser.mqttnotify.domain.repo.AppStateRepository

@HiltViewModel
class AppChromeViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()
    private val _themePreference = MutableStateFlow(ThemePreference.SYSTEM)
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    init {
        viewModelScope.launch {
            appStateRepository.observeState().collect { state ->
                val now = timeProvider.nowMillis()
                _muted.value = state.globalMuteUntil?.let { it > now } ?: false
                _themePreference.value = state.themePreference
            }
        }
    }

    fun toggleMute() {
        viewModelScope.launch {
            if (_muted.value) {
                appStateRepository.setGlobalMuteUntil(null)
            } else {
                appStateRepository.setGlobalMuteUntil(timeProvider.nowMillis() + TEMP_MUTE_MS)
            }
        }
    }

    private companion object {
        private const val TEMP_MUTE_MS = 15 * 60 * 1000L
    }
}
