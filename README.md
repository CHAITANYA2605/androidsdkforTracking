# Android Event Tracking and Push SDK

A multi-module Kotlin/Android project for collecting product events, batching
and retrying delivery, registering devices, and receiving Firebase Cloud
Messaging notifications.

## Modules

| Module | Purpose |
| --- | --- |
| `app` | Example Android application |
| `androidsdk` | Lightweight event queue and retry worker |
| `androidsdkclaude` | Configurable tracker with persistence, batching, lifecycle events, and user identity |
| `pushsdk` | Device registration, FCM handling, notification display, and engagement tracking |

## Development setup

- Android Studio
- A compatible Android SDK
- JDK supported by the Gradle version in this repository
- Firebase configuration for push-notification testing

```bash
git clone https://github.com/CHAITANYA2605/androidsdkforTracking.git
cd androidsdkforTracking
./gradlew build
```

Run unit tests with:

```bash
./gradlew test
```

## Event tracker example

The configurable tracker is initialized with a `TrackerConfig`, then given a
user ID before events are flushed:

```kotlin
val tracker = EventTracker.initialize(
    applicationContext,
    TrackerConfig(
        apiKey = "your-api-key",
        endpoint = "https://your-api.example.com"
    )
)

tracker.setUserId("user-123")
tracker.track("checkout_started", mapOf("cartValue" to 1499))
```

See the source models and public methods for the current constructor fields.

## Data flow

```text
Android app → local queue → batched HTTP delivery → ingestion service
          └→ FCM token/device registration → push backend
```

## Security

Do not commit API secrets or Firebase service-account credentials. Android
client configuration should be restricted to the expected package name and
authorized APIs.
