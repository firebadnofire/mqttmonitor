package org.archuser.mqttnotify.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.archuser.mqttnotify.core.BatteryOptimizationController
import org.archuser.mqttnotify.domain.model.BatteryOptimizationState
import org.archuser.mqttnotify.core.TimeProvider
import org.archuser.mqttnotify.domain.model.ThemePreference
import org.archuser.mqttnotify.domain.repo.AppStateRepository

@HiltViewModel
class AppChromeViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val batteryOptimizationController: BatteryOptimizationController,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()
    private val _themePreference = MutableStateFlow(ThemePreference.SYSTEM)
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()
    private val _batteryOptimizationState = MutableStateFlow(BatteryOptimizationState.UNKNOWN)
    val batteryOptimizationState: StateFlow<BatteryOptimizationState> = _batteryOptimizationState.asStateFlow()
    private val _showBatteryOptimizationPrompt = MutableStateFlow(false)
    val showBatteryOptimizationPrompt: StateFlow<Boolean> = _showBatteryOptimizationPrompt.asStateFlow()
    private var batteryPromptCompleted: Boolean = false

    init {
        viewModelScope.launch {
            appStateRepository.observeState().collect { state ->
                val now = timeProvider.nowMillis()
                _muted.value = state.globalMuteUntil?.let { it > now } ?: false
                _themePreference.value = state.themePreference
                batteryPromptCompleted = state.batteryOptimizationPromptCompleted
                refreshBatteryOptimizationState()
            }
        }
    }

    fun extendMute() {
        viewModelScope.launch {
            val now = timeProvider.nowMillis()
            val currentUntil = appStateRepository.currentState().globalMuteUntil
            val base = currentUntil?.takeIf { it > now } ?: now
            appStateRepository.setGlobalMuteUntil(base + TEMP_MUTE_MS)
        }
    }

    fun refreshBatteryOptimizationState() {
        val state = batteryOptimizationController.state()
        _batteryOptimizationState.value = state
        _showBatteryOptimizationPrompt.value =
            !batteryPromptCompleted && state != BatteryOptimizationState.UNRESTRICTED
    }

    fun requestIgnoreBatteryOptimizations() {
        batteryOptimizationController.requestIgnoreBatteryOptimizations()
        markBatteryOptimizationPromptCompleted()
    }

    fun openBatteryOptimizationSettings() {
        batteryOptimizationController.requestIgnoreBatteryOptimizations()
    }

    fun dismissBatteryOptimizationPrompt() {
        markBatteryOptimizationPromptCompleted()
    }

    private fun markBatteryOptimizationPromptCompleted() {
        _showBatteryOptimizationPrompt.value = false
        viewModelScope.launch {
            appStateRepository.setBatteryOptimizationPromptCompleted(true)
        }
    }

    private companion object {
        private const val TEMP_MUTE_MS = 15 * 60 * 1000L
    }
}
