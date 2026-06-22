package org.archuser.mqttnotify.ui.navigation

object Routes {
    const val CHANNELS = "channels"
    const val CHANNEL_DETAIL = "channel/{channelId}"
    const val CHANNEL_EDIT = "channel_edit/{channelId}"
    const val BROKERS = "brokers"
    const val BROKER_EDIT = "broker_edit/{brokerId}"
    const val SERVICE_STATUS = "service_status"
    const val SETTINGS = "settings"
    const val ABOUT = "about"

    fun channelDetail(channelId: Long) = "channel/$channelId"
    fun channelEdit(channelId: Long) = "channel_edit/$channelId"
    fun brokerEdit(brokerId: Long) = "broker_edit/$brokerId"
}
