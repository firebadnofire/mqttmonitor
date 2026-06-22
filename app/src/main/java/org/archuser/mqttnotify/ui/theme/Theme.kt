package org.archuser.mqttnotify.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import org.archuser.mqttnotify.domain.model.ThemePreference

private val LightColors = lightColors(
    primary = Teal700,
    primaryVariant = Teal900,
    secondary = Teal700,
    error = ErrorRed
)

private val DarkColors = darkColors(
    primary = Teal300,
    primaryVariant = Teal700,
    secondary = Teal300,
    error = ErrorRed
)

@Composable
fun MqttNotifyTheme(
    themePreference: ThemePreference,
    content: @Composable () -> Unit
) {
    val dark = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    MaterialTheme(colors = colors, content = content)
}
