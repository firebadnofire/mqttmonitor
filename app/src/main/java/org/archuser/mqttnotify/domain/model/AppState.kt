package org.archuser.mqttnotify.domain.model

data class AppState(
    val activeBrokerId: Long?,
    val connectionMode: ConnectionMode,
    val globalMuteUntil: Long?,
    val lastSessionStartedAt: Long?,
    val themePreference: ThemePreference,
    val batteryOptimizationPromptCompleted: Boolean,
    val startListenerOnAppLaunch: Boolean,
    val startListenerOnPhoneUnlock: Boolean
)

fun AppState.isMuted(now: Long): Boolean {
    val until = globalMuteUntil ?: return false
    return until > now
}
