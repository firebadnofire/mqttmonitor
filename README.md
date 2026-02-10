# MQTT Monitor

A friendly MQTT notification app for people who run their own broker.

## Play Store Style Description

### What it is

MQTT Monitor is designed for self-hosters, homelab users, and developers who want direct broker monitoring on Android without confusing background behavior, as well as people who want a self-hostable alternative to FCM or tools like [ntfy.sh](https://ntfy.sh).

It helps you keep an eye on topics, receive alerts, and decide how persistent the connection should be based on how you use your phone.

### What you can do

- Connect to your own MQTT broker
- Save multiple brokers and switch quickly
- Test a broker connection before saving it
- Subscribe to topics and enable per-topic alerts
- View retained-message status clearly
- Read and manage messages in a local feed
- Delete individual messages
- Use temporary global mute with selectable durations
- Enable or disable Material You styling

### Two clear connection modes

- **Active While Visible (default):** Connects only while app is on screen
- **Persistent Foreground Service (optional):** Keeps connection running with a persistent notification

### Why people like it

- Clear connection model
- Local-first message history
- Strong control over notifications
- Works well for homelab and debugging workflows

## Screenshots (Placeholders)

> Replace these placeholder paths with real screenshots when available.

<details>
<summary>Dashboard</summary>

![Dashboard Placeholder](docs/images/dashboard-placeholder.png)

</details>

<details>
<summary>Brokers</summary>

![Brokers Placeholder](docs/images/brokers-placeholder.png)

</details>

<details>
<summary>Broker Editor</summary>

![Broker Editor Placeholder](docs/images/broker-edit-placeholder.png)

</details>

<details>
<summary>Topics</summary>

![Topics Placeholder](docs/images/topics-placeholder.png)

</details>

<details>
<summary>Message Feed</summary>

![Message Feed Placeholder](docs/images/messages-placeholder.png)

</details>

<details>
<summary>Settings</summary>

![Settings Placeholder](docs/images/settings-placeholder.png)

</details>

<details>
<summary>Diagnostics</summary>

![Diagnostics Placeholder](docs/images/diagnostics-placeholder.png)

</details>

<details>
<summary>Notification Banner + Persistent Notification</summary>

![Notification Placeholder](docs/images/notifications-placeholder.png)

</details>

---

## Technical Specification

### App ID and platform

- Application ID: `org.archuser.mqttnotify`
- Android min SDK: `26`
- Android target/compile SDK: `36`

### Core stack

- Kotlin + Jetpack Compose
- Hilt (dependency injection)
- Room (local persistence)
- HiveMQ MQTT client
- AndroidX Security Crypto (credential storage)

### Feature set

- MQTT 3.1.1 and MQTT 5.0 support
- Multiple saved brokers
- Single active broker connection at a time
- TLS/auth-capable broker config
- Required broker connection test before save
- Per-topic subscriptions and notification settings
- Global mute (notifications only)
- Retained-message handling controls
- Foreground service mode with persistent notification

### Data model

Room tables:

- `brokers`
- `topic_subscriptions`
- `messages`
- `topic_counters`
- `retention_policies`
- `app_state`

### Project layout

Main source path: `app/src/main/java/org/archuser/mqttnotify/`

- `core/`
- `data/local/`
- `data/mqtt/`
- `data/repo/`
- `data/security/`
- `domain/model/`
- `domain/repo/`
- `connection/`
- `notifications/`
- `service/`
- `ui/`
- `di/`

### Permissions

Defined in `app/src/main/AndroidManifest.xml`:

- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `POST_NOTIFICATIONS`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_DATA_SYNC`

### Build

```bash
./gradlew :app:assembleDebug
```

### Tests

```bash
./gradlew :app:testDebugUnitTest
```

### Combined validation command

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

### Notes

- Background delivery is best-effort, not guaranteed.
- Notification channel behavior is ultimately user-controlled by Android system settings.

## License

License not yet defined.
