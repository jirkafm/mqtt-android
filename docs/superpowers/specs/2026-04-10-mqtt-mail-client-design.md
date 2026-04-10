# MQTT Mail Client Design

## Summary

Build an Android application that behaves like a lightweight email client for MQTT. The app connects to a single MQTT broker account, subscribes to multiple user-defined topics, stores received messages per topic, and notifies the user when new messages arrive. The app is designed to keep the MQTT connection alive while in the background by using a persistent foreground service.

## Goals

- Support one MQTT broker account configuration.
- Support multiple topic subscriptions under that account.
- Store received messages locally with per-topic history.
- Notify the user whenever a new message arrives on a subscribed topic.
- Keep the MQTT connection active while the app is in the background and while the phone is locked.
- Let the user clear message history for an individual subscribed topic without removing the subscription.

## Non-Goals

- Multiple broker accounts.
- Custom authentication schemes beyond standard username/password.
- Client certificate management UI.
- A server-side relay or push-notification backend.
- Guaranteed delivery under all vendor-specific battery optimization policies.

## Product Model

The app behaves like a mail client with these equivalents:

- Mail account -> MQTT broker account
- Mail folder/mailbox -> subscribed topic
- Email message -> MQTT message received on a topic
- Mail sync engine -> foreground MQTT connection service

The user sets up a single broker account, adds multiple topics to monitor, and then leaves the app running its sync engine in the background. Each topic has its own message history screen, similar to opening a mailbox or thread view in a mail client.

## User Experience

### Account Setup

The user configures:

- Broker host
- Broker port
- Whether TLS is enabled
- Username
- Password
- Client ID
- Keepalive interval
- Session persistence setting

This configuration is stored locally and can be edited later from settings.

### Topic Management

The user can:

- Add a topic subscription
- Edit an existing subscription
- Remove a subscription
- Give a topic a friendly display name
- Choose QoS for the subscription
- Enable or disable notifications per topic
- Clear all stored messages for a topic while keeping the subscription active

### Inbox-Style UI

The main screen lists subscribed topics. Each row shows:

- Topic display name or raw topic
- Connection/subscription state if relevant
- Latest received message preview
- Timestamp of latest message
- Unread count

Selecting a topic opens its history screen, which displays received messages in reverse chronological order. Opening a topic marks its messages as read.

### Notifications

When a new message arrives for a topic with notifications enabled:

- The message is persisted locally first
- A user-visible notification is posted
- Tapping the notification opens the corresponding topic history screen

The app also keeps an ongoing foreground-service notification that communicates connection state, such as connected, reconnecting, or disconnected.

## Architecture

### High-Level Components

1. UI layer
   - Jetpack Compose screens for setup, topic list, topic detail, and settings
2. Data layer
   - Room database for broker settings, topics, and received messages
3. Domain/service layer
   - MQTT connection manager and foreground sync service
4. Notification layer
   - Notification channel setup, foreground notification, and per-message notifications
5. System integration
   - Boot receiver and startup wiring for restoring sync after device reboot

### Foreground Sync Service

The foreground service is the owner of the MQTT connection lifecycle. It:

- Starts when the user enables syncing
- Establishes the MQTT connection using the stored broker configuration
- Subscribes to all enabled topics
- Listens for incoming messages continuously
- Persists each message to the database
- Triggers notifications for eligible topics
- Updates the persistent notification with connection status
- Detects disconnects and retries with backoff
- Re-subscribes after successful reconnect

This is the recommended Android-supported model for work that must remain active while the device is locked or the app is not visible.

### MQTT Client Behavior

The app uses a standard MQTT client library that supports:

- MQTT 3.x/5.x compatible broker connectivity as available through the chosen Android-friendly library
- Username/password authentication
- TLS enablement without custom certificate management
- Automatic reconnect hooks where practical

The app should maintain a single live broker connection because there is only one configured account.

### Storage Model

Room stores:

- Broker account configuration
- Topic subscriptions and preferences
- Received messages keyed by topic
- Read/unread state for messages

Each received message record includes:

- Topic ID
- Original topic string
- Payload body
- Received timestamp
- QoS if available
- Retained flag if available
- Read/unread state

The storage schema should allow efficient retrieval of:

- Topic list with latest message preview
- Unread counts per topic
- Full message history for one topic

### Notification Model

Use Android notification channels for:

- Background connection status
- Incoming MQTT message notifications

Per-topic notifications are controlled by the topic configuration. The foreground-service notification is always present while sync is active.

## Data Flow

### Incoming Message Flow

1. Foreground service receives a message from the MQTT client callback
2. Message is mapped into the local persistence model
3. Message is saved to the database
4. Topic unread count is updated implicitly from unread messages
5. If topic notifications are enabled, a notification is posted
6. UI observes database changes and updates automatically

### App Launch Flow

1. App loads broker settings and topic subscriptions from the database
2. UI shows configured topics and latest local history
3. If syncing is enabled, foreground service is started or rebound
4. Service connects and restores subscriptions

### Reboot Flow

1. Device reboot completes
2. Boot receiver checks whether syncing was enabled previously
3. If enabled, it restarts the foreground service
4. Service reconnects and restores subscriptions

## Error Handling

### Connection Failures

If the broker is unreachable or credentials are invalid:

- Keep the service alive if sync is enabled
- Show disconnected/retrying state in the foreground notification and UI
- Retry with bounded exponential backoff
- Avoid crashing the app or losing local history

### Subscription Failures

If one topic fails to subscribe:

- Record the failure state for that topic
- Leave the rest of the topics active
- Show the error in UI where practical
- Retry subscription on later reconnect attempts

### Storage Failures

If message persistence fails:

- Log the failure
- Avoid showing a message notification that cannot be opened from history
- Preserve service stability and continue processing future messages

## Android Constraints And Expectations

The app targets reliable always-on listening within normal Android platform constraints by using a foreground service. This is the best-fit approach for keeping an MQTT connection alive in the background, but exact behavior can still vary across aggressive OEM battery-management implementations. The app should communicate this honestly in settings/help text rather than promising perfect delivery under every device policy.

## Testing Strategy

### Unit Tests

Cover:

- Topic/message repository behavior
- Unread count and mark-as-read logic
- Clear-history behavior per topic
- Notification routing decisions
- Reconnect/backoff policy logic

### Instrumentation / Integration Tests

Cover:

- Room persistence integration
- Foreground service startup wiring
- Deep linking from notifications into topic history
- Basic UI flows for account setup, topic list, topic detail, and clear history

### Manual Validation

Validate on a real device or emulator:

- Receive message while app is foregrounded
- Receive message while app is backgrounded
- Receive message while screen is locked
- Reconnect after temporary network loss
- Restart after device reboot

## Recommended Tech Stack

- Kotlin
- Android app with Jetpack Compose UI
- Room for local persistence
- Android foreground service for background connectivity
- Android notifications
- WorkManager only for supporting maintenance/recovery work if needed, not as the primary always-on MQTT connection mechanism
- An Android-compatible MQTT client library

## File And Module Direction

The implementation should keep these responsibilities separate:

- `ui/` for Compose screens and view models
- `data/` for Room entities, DAOs, and repositories
- `mqtt/` for broker connection wrapper and subscription logic
- `service/` for foreground service orchestration
- `notifications/` for channel and notification builders
- `receiver/` for reboot/startup handling

The exact package names can follow standard Android conventions once the project is scaffolded.

## Open Decisions Resolved

- Single broker account only
- Standard username/password auth only
- Persistent foreground notification is acceptable and recommended
- Multiple topic subscriptions supported
- Per-topic message history required
- Per-topic clear-history action required

## Success Criteria

The first working version is successful when:

- A user can configure one MQTT broker account
- A user can add multiple topic subscriptions
- The app receives messages for subscribed topics
- Incoming messages appear in the correct topic history
- Notifications are shown for new messages
- The MQTT connection remains active through a foreground service while the device is locked in normal Android conditions
- The user can clear stored history for a topic without removing the topic subscription
