package org.archuser.mqttnotify.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.archuser.mqttnotify.domain.model.ConnectionMode
import org.archuser.mqttnotify.domain.model.ConnectionStatus
import org.archuser.mqttnotify.service.PersistentConnectionService
import org.archuser.mqttnotify.ui.screen.AboutScreen
import org.archuser.mqttnotify.ui.screen.BrokerEditScreen
import org.archuser.mqttnotify.ui.screen.BrokerListScreen
import org.archuser.mqttnotify.ui.screen.ChannelDetailScreen
import org.archuser.mqttnotify.ui.screen.ChannelEditScreen
import org.archuser.mqttnotify.ui.screen.ChannelListScreen
import org.archuser.mqttnotify.ui.screen.DiagnosticsScreen
import org.archuser.mqttnotify.ui.screen.SettingsScreen
import org.archuser.mqttnotify.ui.viewmodel.AppChromeViewModel
import org.archuser.mqttnotify.ui.viewmodel.BrokerEditViewModel
import org.archuser.mqttnotify.ui.viewmodel.BrokerListViewModel
import org.archuser.mqttnotify.ui.viewmodel.DashboardViewModel
import org.archuser.mqttnotify.ui.viewmodel.DiagnosticsViewModel
import org.archuser.mqttnotify.ui.viewmodel.SettingsViewModel
import org.archuser.mqttnotify.ui.viewmodel.TopicViewModel

@Composable
fun AppNav(appChromeViewModel: AppChromeViewModel) {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route ?: Routes.CHANNELS
    val muted by appChromeViewModel.muted.collectAsStateWithLifecycle()
    val batteryOptimizationState by appChromeViewModel.batteryOptimizationState.collectAsStateWithLifecycle()
    val dashboardVm: DashboardViewModel = hiltViewModel()
    val dashboardState by dashboardVm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var overflowExpanded by remember { mutableStateOf(false) }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun startListener() {
        if (dashboardState.mode == ConnectionMode.PERSISTENT_FOREGROUND) {
            PersistentConnectionService.start(context)
        } else {
            dashboardVm.setMode(ConnectionMode.VISIBLE_ONLY)
        }
        toast("Listener start requested")
    }

    fun stopListener() {
        PersistentConnectionService.stop(context)
        dashboardVm.setMode(ConnectionMode.VISIBLE_ONLY)
        toast("Listener stopped")
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text(routeTitle(currentRoute)) },
                navigationIcon = if (currentRoute != Routes.CHANNELS) {
                    {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                } else {
                    null
                },
                actions = {
                    val statusIcon = when (dashboardState.snapshot.status) {
                        ConnectionStatus.ERROR -> Icons.Default.Warning
                        else -> Icons.Default.Link
                    }
                    IconButton(onClick = { navController.navigate(Routes.SERVICE_STATUS) }) {
                        Icon(statusIcon, contentDescription = "Listener status")
                    }
                    IconButton(
                        onClick = {
                            appChromeViewModel.extendMute()
                            toast(if (muted) "Notifications muted for 15 more minutes" else "Notifications muted for 15 minutes")
                        }
                    ) {
                        Icon(
                            imageVector = if (muted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                            contentDescription = "Mute notifications for 15 minutes"
                        )
                    }
                    IconButton(onClick = { overflowExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = overflowExpanded, onDismissRequest = { overflowExpanded = false }) {
                        DropdownMenuItem(onClick = {
                            overflowExpanded = false
                            navController.navigate(Routes.BROKERS)
                        }) { Text("Brokers") }
                        DropdownMenuItem(onClick = {
                            overflowExpanded = false
                            navController.navigate(Routes.SERVICE_STATUS)
                        }) { Text("Service status") }
                        DropdownMenuItem(onClick = {
                            overflowExpanded = false
                            navController.navigate(Routes.SETTINGS)
                        }) { Text("Settings") }
                        DropdownMenuItem(onClick = {
                            overflowExpanded = false
                            navController.navigate(Routes.ABOUT)
                        }) { Text("About") }
                        DropdownMenuItem(onClick = {
                            overflowExpanded = false
                            if (dashboardState.snapshot.status == ConnectionStatus.CONNECTED) stopListener() else startListener()
                        }) {
                            Text(if (dashboardState.snapshot.status == ConnectionStatus.CONNECTED) "Stop listener" else "Start listener")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentRoute == Routes.CHANNELS) {
                FloatingActionButton(
                    modifier = Modifier.navigationBarsPadding(),
                    onClick = {
                        if (dashboardState.brokers.isEmpty()) {
                            navController.navigate(Routes.brokerEdit(0L))
                        } else {
                            navController.navigate(Routes.channelEdit(0L))
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.CHANNELS,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Routes.CHANNELS) {
                val vm: TopicViewModel = hiltViewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                ChannelListScreen(
                    state = state,
                    brokerCount = dashboardState.brokers.size,
                    snapshot = dashboardState.snapshot,
                    onAddBroker = { navController.navigate(Routes.brokerEdit(0L)) },
                    onAddChannel = { navController.navigate(Routes.channelEdit(0L)) },
                    onOpenChannel = { navController.navigate(Routes.channelDetail(it)) },
                    onEditChannel = { navController.navigate(Routes.channelEdit(it)) },
                    onDeleteChannel = {
                        vm.deleteChannel(it)
                        toast("Channel deleted")
                    },
                    onStartListener = ::startListener,
                    onViewService = { navController.navigate(Routes.SERVICE_STATUS) }
                )
            }

            composable(
                route = Routes.CHANNEL_DETAIL,
                arguments = listOf(navArgument("channelId") { type = NavType.StringType })
            ) {
                val vm: TopicViewModel = hiltViewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                ChannelDetailScreen(
                    state = state,
                    onClearHistory = {
                        vm.clearChannelHistory()
                        toast("Channel history cleared")
                    },
                    onMarkAllRead = {
                        vm.markAllRead()
                        toast("Channel marked read")
                    },
                    onDeleteMessage = {
                        vm.deleteMessage(it)
                        toast("Message deleted")
                    }
                )
            }

            composable(
                route = Routes.CHANNEL_EDIT,
                arguments = listOf(navArgument("channelId") { type = NavType.StringType })
            ) {
                val vm: TopicViewModel = hiltViewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                ChannelEditScreen(
                    state = state,
                    onChange = vm::updateForm,
                    onSave = {
                        vm.saveChannel {
                            navController.popBackStack()
                            toast("Channel saved")
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }

            composable(Routes.BROKERS) {
                val vm: BrokerListViewModel = hiltViewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                BrokerListScreen(
                    state = state,
                    onAdd = { navController.navigate(Routes.brokerEdit(0L)) },
                    onEdit = { navController.navigate(Routes.brokerEdit(it)) },
                    onDelete = {
                        vm.deleteBroker(it)
                        toast("Broker deleted")
                    }
                )
            }

            composable(
                route = Routes.BROKER_EDIT,
                arguments = listOf(navArgument("brokerId") { type = NavType.StringType })
            ) {
                val vm: BrokerEditViewModel = hiltViewModel()
                val state by vm.state.collectAsStateWithLifecycle()

                LaunchedEffect(state.status) {
                    if (!state.status.isNullOrBlank()) toast(state.status ?: "")
                }

                BrokerEditScreen(
                    state = state,
                    onChange = { newState -> vm.update { newState.copy(tls = true) } },
                    onTest = vm::testConnection,
                    onSave = {
                        vm.save {
                            navController.popBackStack()
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }

            composable(Routes.SERVICE_STATUS) {
                val vm: DiagnosticsViewModel = hiltViewModel()
                val snapshot by vm.snapshot.collectAsStateWithLifecycle()
                val events by vm.events.collectAsStateWithLifecycle()
                DiagnosticsScreen(
                    snapshot = snapshot,
                    events = events,
                    mode = dashboardState.mode,
                    onPersistentListenerChanged = { enabled ->
                        if (enabled) {
                            PersistentConnectionService.start(context)
                            dashboardVm.setMode(ConnectionMode.PERSISTENT_FOREGROUND)
                        } else {
                            PersistentConnectionService.stop(context)
                            dashboardVm.setMode(ConnectionMode.VISIBLE_ONLY)
                        }
                    },
                    onStartStop = {
                        if (snapshot.status == ConnectionStatus.CONNECTED) stopListener() else startListener()
                    }
                )
            }

            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = hiltViewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                SettingsScreen(
                    state = state,
                    batteryOptimizationState = batteryOptimizationState,
                    onMuteForMinutes = {
                        vm.muteFor(it)
                        toast("Notifications muted for $it minutes")
                    },
                    onClearMute = {
                        vm.clearMute()
                        toast("Notification mute cleared")
                    },
                    onThemeChanged = vm::setThemePreference,
                    onKeepHistoryIndefinitelyChanged = vm::setKeepHistoryIndefinitely,
                    onPersistentListenerChanged = { enabled ->
                        if (enabled) {
                            PersistentConnectionService.start(context)
                            dashboardVm.setMode(ConnectionMode.PERSISTENT_FOREGROUND)
                        } else {
                            PersistentConnectionService.stop(context)
                            dashboardVm.setMode(ConnectionMode.VISIBLE_ONLY)
                        }
                    },
                    onStartListenerOnAppLaunchChanged = vm::setStartListenerOnAppLaunch,
                    onStartListenerOnPhoneUnlockChanged = vm::setStartListenerOnPhoneUnlock,
                    onOpenBatterySettings = appChromeViewModel::openBatteryOptimizationSettings
                )
            }

            composable(Routes.ABOUT) {
                AboutScreen()
            }
        }
    }
}

private fun routeTitle(route: String): String = when {
    route.startsWith("channels") -> "Channels"
    route.startsWith("channel/") -> "Channel"
    route.startsWith("channel_edit") -> "Channel"
    route.startsWith("brokers") -> "Brokers"
    route.startsWith("broker_edit") -> "Broker"
    route.startsWith("service_status") -> "Service status"
    route.startsWith("settings") -> "Settings"
    route.startsWith("about") -> "About"
    else -> "MQTT Notify"
}
