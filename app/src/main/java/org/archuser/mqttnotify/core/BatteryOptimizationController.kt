package org.archuser.mqttnotify.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.archuser.mqttnotify.domain.model.BatteryOptimizationState

@Singleton
class BatteryOptimizationController @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun state(): BatteryOptimizationState {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return BatteryOptimizationState.UNKNOWN
        return try {
            if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                BatteryOptimizationState.UNRESTRICTED
            } else {
                BatteryOptimizationState.OPTIMIZED
            }
        } catch (_: RuntimeException) {
            BatteryOptimizationState.UNKNOWN
        }
    }

    fun requestIgnoreBatteryOptimizations() {
        val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(requestIntent)
        } catch (_: ActivityNotFoundException) {
            openAppBatterySettings()
        }
    }

    fun openAppBatterySettings() {
        val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallbackIntent)
    }
}
