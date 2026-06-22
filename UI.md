# UI.md: MQTT Notify Android UI/UX Specification

**App name:** MQTT Notify
**Android package ID:** `org.archuser.mqttnotify`
**UI stack:** Kotlin, Jetpack Compose, Material 2
**Design target:** A clean Android notification-terminal interface for technically literate users.

MQTT Notify is not a chat app, not a dashboard, and not a smart-home control panel. Its UI should feel like a simple Android utility for monitoring configured MQTT channels and receiving local notifications.

The app should be plain, fast, readable, and honest about connection state.

---

## 1. Core UI Model

MQTT Notify has four primary user-facing objects.

### 1.1 Broker

A broker is an MQTT server connection target.

Examples:

```text
mqtts://mqtt.example.org:8883
mqtts://192.168.1.50:8883
```

A broker contains:

* Display name
* Hostname or IP address
* Port
* TLS state
* Username/password, if configured
* Enabled/disabled state
* Last connection result
* Last error, if any

All enabled brokers are checked by the listener service.

### 1.2 Channel

A channel is a user-configured notification stream.

A channel contains:

* Display name
* One or more MQTT topic filters
* Notification enabled/muted state
* Retained-message behavior
* Local history
* Android notification channel mapping

Examples:

```text
Alerts
Backups
Garage
Server Health
```

A channel is not tied to one broker. Every enabled broker is checked for every enabled channel topic filter.

### 1.3 Topic Filter

A topic filter is the MQTT subscription pattern used by a channel.

Examples:

```text
william/important
shared/garage
home/+/temperature
alerts/#
```

MQTT wildcard filters must be supported.

Supported wildcard behavior:

* `+` matches one topic level
* `#` matches all remaining topic levels
* `#` is only valid as the final topic level
* Topic filters must be validated before saving

### 1.4 Channel Feed

A channel feed is the local notification history for one channel.

Messages from all enabled brokers that match the channel’s topic filters are merged into that channel feed.

The user should think:

```text
I have an Alerts channel.
It listens for these MQTT topics across all enabled brokers.
```

The user should not have to think:

```text
I have a separate subscription list for each broker.
```

---

## 2. Visual Design

### 2.1 Design Language

MQTT Notify should use a simple Material 2 Android design.

The visual style should be:

* Plain
* Dense enough to be useful
* Not playful
* Not heavily rounded
* Not dashboard-like
* Not card-heavy
* Not visually noisy

The app should resemble a classic Android utility:

* Top app bar
* Text-based list rows
* Floating action button
* Simple dialogs
* Full-screen editors for complex forms
* Overflow menus for secondary actions

### 2.2 Material Version

Use **Material 2**, not Material 3.

Material 3’s large rounded surfaces, oversized pills, and softer visual language do not fit this app. MQTT Notify should look like a direct system utility, not a lifestyle app.

### 2.3 Theme

MQTT Notify must support:

* Light theme
* Dark theme
* Follow system theme

The default setting is:

```text
Theme: Follow system
```

Theme options are available in Settings:

```text
Theme
- Follow system
- Light
- Dark
```

### 2.4 Colors

Use a restrained primary color.

Recommended default:

```text
Primary: deep teal/green
```

The color should be similar in spirit to older Android utility apps:

* Top app bar uses primary color
* FAB uses primary color
* Active checkboxes/switches use primary color
* Error states use red
* Warning states use amber/yellow
* Disabled states use gray

Do not use bright gradients, pastel backgrounds, or decorative illustrations.

### 2.5 Typography

Use the system default font for normal UI text.

Use monospace only for:

* Raw MQTT topic filters
* Broker host strings
* Payload detail blocks
* Error detail blocks
* Client IDs

Do not make the entire app monospace.

### 2.6 Icons

Icons should be simple and functional.

Recommended icon meanings:

* Channel: message bubble or terminal/message icon
* Broker: server icon
* Connected: check or link icon
* Connecting: sync/progress icon
* Error: warning triangle
* Muted: crossed-out bell
* Notifications enabled: bell
* Retained message: small bookmark/history marker
* Persistent listener active: lightning bolt or link/activity icon

Icons should support text. Do not rely on icons alone for critical state.

---

## 3. Navigation Structure

MQTT Notify uses a simple stack navigation model.

There is no bottom navigation bar.

Primary screens:

1. Onboarding
2. Channels
3. Channel Detail
4. Add/Edit Channel
5. Brokers
6. Add/Edit Broker
7. Service Status
8. Settings
9. About

### 3.1 Main Navigation

The app opens to the **Channels** screen after onboarding.

Secondary screens are reached through:

* Floating action button
* Overflow menu
* Tapping list rows
* Error banners
* Notification taps

### 3.2 Overflow Menu on Main Screen

The main screen overflow menu contains:

```text
Brokers
Service status
Settings
About
```

If the listener is active, include:

```text
Stop listener
```

If the listener is stopped, include:

```text
Start listener
```

---

## 4. Onboarding

Onboarding is shown on first launch.

The user must be able to skip onboarding only after the app has enough configuration to be useful. A user who has not configured a broker or channel should be returned to onboarding or shown an empty setup state.

Onboarding has five steps.

---

### 4.1 Step 1: Welcome

Title:

```text
MQTT Notify
```

Body:

```text
MQTT Notify listens to your MQTTS brokers and shows local notifications for the channels you configure.
```

Additional text:

```text
It runs as a foreground service when background listening is enabled. Android may still restrict background work depending on your system settings.
```

Primary action:

```text
GET STARTED
```

---

### 4.2 Step 2: Add Broker

Title:

```text
Add broker
```

Fields:

```text
Name
Host
Port
Username
Password
```

Defaults:

```text
Port: 8883
TLS: Required
```

TLS is required for v1. There is no plain MQTT toggle.

The screen includes:

```text
SAVE
TEST CONNECTION
```

Saving and testing are separate.

A broker may be saved without a successful test. If saved without a test, it appears as:

```text
Not tested
```

If the connection test fails, show the specific error.

Examples:

```text
DNS lookup failed
Connection refused
TLS handshake failed
Authentication failed
Connection timed out
Broker rejected client ID
```

The user may continue after saving a broker, even if it has not been tested.

---

### 4.3 Step 3: Add First Channel

Title:

```text
Add channel
```

Fields:

```text
Channel name
Topic filter
```

Examples shown under the topic field:

```text
william/important
shared/garage
home/+/temperature
alerts/#
```

Options:

```text
Notify for new messages
Hide retained messages from history
```

Defaults:

```text
Notify for new messages: on
Hide retained messages from history: on
```

Primary action:

```text
SAVE CHANNEL
```

Validation:

* Channel name cannot be empty
* Topic filter cannot be empty
* Topic filter must be a valid MQTT topic filter
* `#` must be final if present
* Empty topic levels should be rejected unless deliberately supported by the MQTT library and documented later

---

### 4.4 Step 4: Persistence and Storage

Title:

```text
Persistence
```

This step configures how MQTT Notify behaves after setup.

Sections:

#### Background listener

Text:

```text
MQTT Notify can keep listening in the background by running a foreground service.
```

Setting:

```text
Enable persistent listener
```

Default:

```text
On
```

Explanation:

```text
When enabled, MQTT Notify keeps a persistent notification visible while it listens. If the notification is gone, the listener is stopped.
```

#### Message history

Text:

```text
MQTT Notify stores channel history locally on this device.
```

Setting:

```text
Save local channel history
```

Default:

```text
On
```

For v1, this should remain enabled unless the implementation supports notification-only mode cleanly.

Retention setting:

```text
Keep history
- 7 days
- 30 days
- 90 days
- Forever
```

Default:

```text
30 days
```

If the user disables local history, warn:

```text
Without local history, channel feeds will only show messages received during the current app session.
```

For v1, if local history cannot be disabled safely, do not expose the toggle. Only expose retention duration.

---

### 4.5 Step 5: Permissions

Title:

```text
Permissions
```

This screen asks for required Android permissions and settings.

Items:

```text
Notifications
Battery optimization
Foreground service
```

#### Notifications

Text:

```text
Allow notifications so MQTT Notify can alert you when channel messages arrive.
```

Button:

```text
ALLOW NOTIFICATIONS
```

#### Battery optimization

Text:

```text
Android may stop the listener while the phone is idle. Set MQTT Notify to unrestricted battery usage for more reliable listening.
```

Button:

```text
OPEN BATTERY SETTINGS
```

This step is strongly recommended but not an absolute blocker.

If the user skips it, show:

```text
Battery optimization is still enabled. Android may stop the listener in the background.
```

#### Foreground service

Text:

```text
When the listener is active, MQTT Notify shows a persistent status notification. This notification is the indication that the app is actively listening.
```

There is no separate user action unless Android requires one.

Primary action:

```text
CONTINUE
```

---

### 4.6 Step 6: Start Listener

Title:

```text
Ready
```

Summary:

```text
Broker: Home MQTT
Channel: Alerts
Persistence: Enabled
Notifications: Allowed
Battery optimization: Not unrestricted
```

If battery optimization was skipped, keep the warning visible.

Primary action:

```text
START LISTENER
```

Secondary action:

```text
FINISH WITHOUT STARTING
```

---

## 5. Main Screen: Channels

The main screen is the default app screen.

Title:

```text
Channels
```

Top app bar actions:

* Listener status icon
* Overflow menu

Floating action button:

```text
+
```

FAB behavior:

* If no broker exists: open Add Broker
* If one or more brokers exist: open Add menu

Add menu:

```text
Add channel
Add broker
```

---

### 5.1 Channel List Row

Each channel row displays:

* Channel icon
* Channel name
* Most recent message preview
* Last activity time
* Notification/mute state
* New message count badge, if nonzero
* Error marker, if the channel has active errors

Default row layout:

```text
[icon]  Alerts                         14:32
        SSH login: root on server       [3]
```

If there is no message history:

```text
[icon]  Alerts
        No messages yet
```

If the channel is muted:

```text
[icon]  Alerts                    muted icon
        SSH login: root on server
```

If the channel has an error on one or more brokers:

```text
[warning icon] Alerts                  14:32
               1 broker error
```

The row should remain tappable even when there is an error.

Tapping the row opens Channel Detail.

Long-pressing the row opens a context menu:

```text
Mute channel
Edit channel
Clear history
Delete channel
```

If muted, replace `Mute channel` with:

```text
Unmute channel
```

---

### 5.2 Channel Sorting

Default sort:

```text
First added
```

The user may rearrange channels manually.

Sorting options:

```text
Manual
First added
Newest activity
Alphabetical A-Z
Alphabetical Z-A
Most new messages
```

The sorting control lives in the overflow menu:

```text
Sort channels
```

If `Manual` is selected, the list supports drag-and-drop rearranging through a visible drag handle or edit mode.

Manual rearrange mode:

* User opens overflow menu
* Taps `Rearrange channels`
* Drag handles appear on rows
* User taps `DONE`

Do not make normal list rows draggable accidentally. That creates bad touch behavior.

---

### 5.3 Listener State Banner

If the listener is stopped, show a banner at the top of the channel list:

```text
MQTT listener is stopped
No brokers are currently connected.
[START]
```

If the listener is active and healthy, no banner is required.

If the listener is active but degraded, show a warning banner:

```text
1 broker has errors
VPS MQTT: TLS handshake failed
[VIEW]
```

If all brokers are disconnected:

```text
All brokers disconnected
No channels are currently receiving messages.
[VIEW ERRORS]
```

Do not hide service errors inside logs.

---

### 5.4 Empty State

If no channels exist:

Title:

```text
No channels
```

Body:

```text
Add a channel to listen for MQTT topic filters across your enabled brokers.
```

Primary action:

```text
ADD CHANNEL
```

If no brokers exist:

Title:

```text
No brokers
```

Body:

```text
Add an MQTTS broker before creating channels.
```

Primary action:

```text
ADD BROKER
```

---

## 6. Channel Detail Screen

The Channel Detail screen shows local history for one channel.

Top app bar:

```text
<  Alerts                         [bell] [overflow]
```

Top app bar elements:

* Back arrow
* Channel name
* Bell/mute icon
* Overflow menu

Overflow menu:

```text
Edit channel
Mute channel
Clear history
Delete channel
Notification settings
```

If muted, replace `Mute channel` with:

```text
Unmute channel
```

---

### 6.1 Message Order

Messages are shown newest first.

The most recent message appears at the top.

This matches a notification history model and makes the screen useful immediately after opening from a notification.

---

### 6.2 Message Row

Each message row displays:

* Received timestamp
* Payload preview
* Optional warning/retained marker
* Broker name, if needed for clarity
* Topic, if the channel has multiple topic filters or if the topic differs from the primary filter

Basic row:

```text
Tue Jun 22 16:41:03
SSH login: root on server from 192.168.1.20
```

With broker and topic:

```text
Tue Jun 22 16:41:03
SSH login: root on server from 192.168.1.20
Home MQTT · william/important
```

Retained messages are hidden by default. If visible later, retained messages must be clearly marked:

```text
Retained
```

---

### 6.3 Message Detail

Tapping a message opens Message Detail.

Message Detail can be a full screen or bottom sheet. For v1, use a full screen if payloads can be long.

Title:

```text
Message
```

Fields:

```text
Channel
Broker
Topic
Received
Retained
QoS
Payload
```

Payload is plain text only.

Payload display:

* Preserve line breaks
* Use monospace
* Allow text selection
* Provide copy action

Actions:

```text
COPY PAYLOAD
```

Overflow menu:

```text
Copy topic
Copy broker
Delete message
```

---

### 6.4 Clear History

`Clear history` removes all local messages for the channel.

Confirmation dialog:

Title:

```text
Clear history?
```

Body:

```text
This deletes local message history for this channel. Broker subscriptions and notification settings will not change.
```

Actions:

```text
CANCEL
CLEAR
```

---

### 6.5 Delete Channel

Deleting a channel also deletes its local history.

Confirmation dialog:

Title:

```text
Delete channel?
```

Body:

```text
This deletes the channel and all local history stored for it. This cannot be undone.
```

Actions:

```text
CANCEL
DELETE
```

`DELETE` must use destructive styling.

---

## 7. Add/Edit Channel Screen

Use a full-screen editor, not a tiny dialog.

Title:

```text
Add channel
```

or:

```text
Edit channel
```

Fields:

```text
Channel name
Topic filters
```

Topic filters are displayed as a list.

Each topic filter row:

```text
william/important        [remove]
```

Add topic filter control:

```text
ADD TOPIC FILTER
```

Topic filter entry field example text:

```text
home/+/temperature
```

Options:

```text
Notify for new messages
Hide retained messages from history
```

Notification section:

```text
Android notification channel
Alerts
[OPEN ANDROID SETTINGS]
```

Buttons:

```text
SAVE
CANCEL
```

Validation:

* Channel name cannot be empty
* At least one topic filter is required
* Topic filters must be valid MQTT topic filters
* Duplicate topic filters inside one channel should be rejected
* Warn if another channel already uses the exact same topic filter

Duplicate warning:

```text
Another channel already listens to this topic filter. Messages may appear in both channels.
```

This is allowed after warning.

---

## 8. Brokers Screen

The Brokers screen lists all configured brokers.

Title:

```text
Brokers
```

Top app bar actions:

* Add broker
* Overflow menu

Broker row fields:

* Broker name
* Host and port
* Connection state
* Enabled/disabled state
* Last error, if any

Connected row:

```text
[server icon] Home MQTT                 Connected
              mqtt.example.org:8883     8 channels active
```

Connecting row:

```text
[server icon] Home MQTT                 Connecting
              mqtt.example.org:8883
```

Disabled row:

```text
[server icon] VPS MQTT                  Disabled
              mqtt.example.org:8883
```

Error row:

```text
[warning icon] VPS MQTT                 TLS error
               mqtt.example.org:8883    TLS handshake failed
```

Tapping a broker opens Broker Detail/Edit.

Long-press menu:

```text
Test connection
Enable broker
Edit broker
Delete broker
```

If enabled, replace `Enable broker` with:

```text
Disable broker
```

---

## 9. Add/Edit Broker Screen

Use a full-screen editor.

Title:

```text
Add broker
```

or:

```text
Edit broker
```

Fields:

```text
Name
Host
Port
Username
Password
Client ID
```

Defaults:

```text
Port: 8883
TLS: Required
Client ID: Auto
```

TLS is required and should be shown as fixed text:

```text
TLS: Required
```

Do not show a plain MQTT option in v1.

Advanced section:

```text
Advanced
Client ID
Keepalive seconds
Connection timeout seconds
```

Defaults:

```text
Client ID: Auto
Keepalive: 60 seconds
Connection timeout: 10 seconds
```

Actions:

```text
SAVE
TEST CONNECTION
```

Testing behavior:

* Test uses the current unsaved form values
* Test result appears inline
* Save does not require test success

Successful test:

```text
Connection successful
```

Failed test examples:

```text
DNS lookup failed
Connection refused
TLS handshake failed
Authentication failed
Connection timed out
Unsupported TLS version
Broker rejected client ID
```

Do not collapse these into generic errors.

---

## 10. Service Status Screen

The Service Status screen shows what the listener is currently doing.

Title:

```text
Service status
```

Sections:

### 10.1 Listener

Fields:

```text
State
Started at
Running for
Persistent listener
Battery optimization
Foreground notification
```

Examples:

```text
State: Active
Running for: 2h 14m
Persistent listener: Enabled
Battery optimization: Unrestricted
Foreground notification: Visible
```

If stopped:

```text
State: Stopped
```

Primary action:

```text
START LISTENER
```

or:

```text
STOP LISTENER
```

### 10.2 Brokers

List each broker state:

```text
Home MQTT
Connected · 8 channels active
```

```text
VPS MQTT
TLS handshake failed
```

### 10.3 Recent Service Events

This is not message history. It is service/runtime history.

Examples:

```text
16:41 Connected to Home MQTT
16:42 Subscribed to alerts/#
16:43 VPS MQTT: TLS handshake failed
```

This screen is for diagnostics only.

---

## 11. Settings Screen

Title:

```text
Settings
```

Sections:

### 11.1 Appearance

```text
Theme
- Follow system
- Light
- Dark
```

### 11.2 Listener

```text
Persistent listener
```

Explanation:

```text
Keep MQTT Notify running as a foreground service when listening in the background.
```

```text
Start listener on app launch
```

Default:

```text
Off
```

Do not enable autostart by default.

### 11.3 Notifications

```text
Global mute
```

Explanation:

```text
Mute notifications without stopping message history.
```

```text
Open Android notification settings
```

### 11.4 History

```text
Keep history
- 7 days
- 30 days
- 90 days
- Forever
```

Default:

```text
30 days
```

```text
Clear all local history
```

Confirmation required.

### 11.5 Battery

```text
Battery optimization
```

State examples:

```text
Unrestricted
Optimized
Unknown
```

Action:

```text
OPEN BATTERY SETTINGS
```

### 11.6 Advanced

```text
Export configuration
Import configuration
Reset onboarding
```

If export/import is not implemented in v1, omit it rather than showing disabled clutter.

---

## 12. Android Notification Channels

MQTT Notify must create one Android notification channel per configured app channel.

Example:

MQTT Notify channel:

```text
Alerts
```

Android notification channel:

```text
MQTT Notify: Alerts
```

The user can configure sound, vibration, and importance through Android system settings.

The app should provide a shortcut from Channel Detail:

```text
Notification settings
```

This opens Android notification settings for that channel if supported.

---

## 13. Foreground Service Notification

MQTT Notify uses a persistent foreground notification when the listener is active.

This notification is the user-visible contract:

```text
If the persistent notification is visible, MQTT Notify is trying to listen.
If it is gone, MQTT Notify is not actively listening.
```

The persistent notification should be silent by default.

### 13.1 Healthy State

Collapsed title:

```text
MQTT Notify is listening
```

Collapsed body:

```text
3 brokers active · 8 channels
```

Expanded body:

```text
Active brokers: 3/3
Channels: 8
Messages received: 42
Last message: Alerts · 14:32
```

Actions:

```text
STOP
MUTE
OPEN
```

### 13.2 Connecting State

Collapsed title:

```text
MQTT Notify is connecting
```

Collapsed body:

```text
Checking Home MQTT…
```

Actions:

```text
STOP
OPEN
```

### 13.3 Degraded State

Collapsed title:

```text
MQTT Notify has connection errors
```

Collapsed body:

```text
2 connected · 1 failed
```

Expanded body:

```text
Active brokers: 2/3
Channels: 8
Last error: VPS MQTT · TLS handshake failed
```

Actions:

```text
RETRY
STOP
OPEN
```

### 13.4 Stopped State

No persistent foreground notification should remain after the listener is stopped.

---

## 14. Message Notifications

Message notifications are generated when a new non-retained message arrives for a channel with notifications enabled.

Retained messages do not produce notifications by default.

Notification title:

```text
Alerts
```

Notification body:

```text
SSH login: root on server from 192.168.1.20
```

Subtext or expanded metadata:

```text
Home MQTT · william/important
```

Actions:

```text
OPEN
MUTE CHANNEL
COPY
```

Behavior:

* Tapping notification opens the Channel Detail screen
* The opened channel should scroll to the triggering message
* `MUTE CHANNEL` mutes future notifications for that channel
* `COPY` copies the payload if Android version and implementation allow it cleanly
* If copy is unreliable from notification actions, omit it rather than implementing a broken action

---

## 15. Mute Behavior

MQTT Notify supports manual mute only in v1.

Mute levels:

* Global mute
* Per-channel mute

Muted behavior:

* Incoming messages are still stored in local history
* Channel rows still update
* New message counts still update
* Android message notifications are not posted

Global mute does not stop the listener.

Per-channel mute does not unsubscribe from the broker.

There are no quiet hours, schedules, rules, or automation in v1.

---

## 16. Retained Message Behavior

Retained messages are hidden from channel history by default.

Default channel option:

```text
Hide retained messages from history: on
```

Retained messages:

* Do not trigger message notifications by default
* Do not count as new activity by default
* May be visible later only if the user changes retained-message settings

If retained messages are shown, they must be marked clearly:

```text
Retained
```

---

## 17. Local History

There is no separate global “recent messages” screen.

History exists inside each channel.

The app has:

```text
Channel list
Channel history inside each channel
```

It does not have:

```text
Global inbox
Global recent messages feed
Search screen
Analytics dashboard
```

Deleting a channel deletes its local history.

Clearing a channel history does not delete the channel.

Retention is configured globally in Settings and during onboarding.

---

## 18. Error UX

User-facing errors must be visible to the user.

Do not hide connection failures inside logs.

Errors appear in:

* Main screen warning banner
* Channel rows, when relevant
* Broker rows
* Service Status screen
* Broker test results

### 18.1 Error Examples

Use specific messages:

```text
DNS lookup failed
Connection refused
Connection timed out
TLS handshake failed
Unsupported TLS version
Authentication failed
Broker rejected client ID
Subscription failed
Socket closed
Network unavailable
```

Bad error text:

```text
Something went wrong
Unable to connect
Error
Unknown failure
```

Generic errors are only acceptable if the underlying exception truly cannot be classified. Even then, include details in a technical detail expander.

### 18.2 Main Screen Error Banner

If one broker has an error:

```text
1 broker has errors
VPS MQTT: TLS handshake failed
[VIEW]
```

If all brokers failed:

```text
All brokers disconnected
No channels are currently receiving messages.
[VIEW ERRORS]
```

If a channel subscription failed:

```text
Alerts has subscription errors
Subscription failed on VPS MQTT.
[VIEW]
```

---

## 19. Permissions UX

Permission prompts should be contextual.

Do not dump all system permission dialogs at once.

The app should explain each permission before launching Android’s system screen.

### 19.1 Notification Permission

Prompt text:

```text
Allow notifications so MQTT Notify can alert you when channel messages arrive.
```

### 19.2 Battery Optimization

Prompt text:

```text
Android may stop the listener while the phone is idle. Set MQTT Notify to unrestricted battery usage for more reliable listening.
```

If skipped:

```text
Battery optimization is still enabled. Android may stop the listener in the background.
```

### 19.3 Foreground Service Notification

Prompt text:

```text
When listening in the background, MQTT Notify shows a persistent notification. This keeps the listener visible and under your control.
```

---

## 20. Tone and Copy Style

MQTT Notify should be technical and approachable.

Use plain language, but do not dumb down MQTT concepts.

Good:

```text
TLS handshake failed
```

Good:

```text
Battery optimization is enabled. Android may stop the listener while the phone is idle.
```

Bad:

```text
Oops! Something went wrong.
```

Bad:

```text
We need a few permissions to make the magic happen.
```

The app should sound like a trustworthy system utility.

---

## 21. V1 Non-Goals in UI

Do not include UI for:

* Publishing messages
* Automation rules
* Scripting
* Smart-home dashboards
* Cloud push relay
* Global search
* JSON formatting
* Hex payload view
* Quiet hours
* Per-channel schedules
* Charts or analytics
* Rich payload rendering
* Public topic discovery
* Social/chat features

Do not show disabled placeholders for these features in v1.

A missing feature is better than a dead button.

---

## 22. Screen Summary

### Onboarding

Purpose:

```text
Configure first broker, first channel, persistence, history, permissions, and listener start.
```

### Channels

Purpose:

```text
Primary app screen. Shows configured channels, latest message previews, new counts, mute state, and visible connection errors.
```

### Channel Detail

Purpose:

```text
Shows local history for one channel, newest first.
```

### Add/Edit Channel

Purpose:

```text
Configure channel name, topic filters, notification behavior, and retained-message behavior.
```

### Brokers

Purpose:

```text
Show configured brokers and their connection state.
```

### Add/Edit Broker

Purpose:

```text
Configure MQTTS connection target and credentials.
```

### Service Status

Purpose:

```text
Show foreground listener state, broker connection state, runtime events, and diagnostics.
```

### Settings

Purpose:

```text
Theme, listener behavior, notification controls, retention, battery settings, and advanced app-level controls.
```

---

## 23. Final Product Shape

MQTT Notify should feel like this:

```text
Home = Channels
Channel = Message history
Broker = Connection backend
Service = Runtime state
Settings = App behavior
```

The app should make its operating state obvious.

If it is listening, the user should know.

If it is stopped, the user should know.

If a broker is broken, the user should know why.

If Android may kill the listener, the user should be warned.

MQTT Notify should be boring in the right way: direct, observable, and reliable within the limits of Android background execution.
