package org.archuser.mqttnotify.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun warningAccentColor(): Color = if (MaterialTheme.colors.isLight) {
    WarningAmberStrong
} else {
    WarningAmber
}

@Composable
fun warningSurfaceColor(): Color = if (MaterialTheme.colors.isLight) {
    WarningAmberSoft
} else {
    WarningAmberStrong.copy(alpha = 0.28f)
}
