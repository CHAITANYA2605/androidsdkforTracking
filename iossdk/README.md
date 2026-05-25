# EventPush iOS SDKs

This folder contains two separate iOS SDKs:

- `EventTrackingIOSSDK`: event tracking, batching, retry, device metadata, user identity, and lifecycle events.
- `PushNotificationIOSSDK`: iOS push permission helpers, FCM/APNs token registration, and notification event handling.

Both SDKs are native Swift Package products and can also be used from Flutter iOS apps through the bridge files under `flutter_ios_bridge`.

## Native iOS Install

Add this folder as a local Swift Package:

```swift
.package(path: "../sdk/iossdk")
```

Then add only the product your app needs:

```swift
.product(name: "EventTrackingIOSSDK", package: "EventPushIOSSDK")
.product(name: "PushNotificationIOSSDK", package: "EventPushIOSSDK")
```

## Event Tracking SDK

```swift
import EventTrackingIOSSDK

let config = TrackerConfig(
    appId: "app_xxx",        // common iOS/Android App ID shown in console
    serverApiKey: "trk_xxx", // server API key from console
    apiBaseUrl: URL(string: "http://localhost:8080")!
)

EventTracker.shared.initialize(config: config)
EventTracker.shared.setUserId("user_123")
EventTracker.shared.track("Product Viewed", properties: [
    "sku": .string("SKU-1"),
    "price": .number(199.0)
])
EventTracker.shared.flush()
```

Events are sent through:

```text
POST /api/v1/events/track
Authorization: Bearer <serverApiKey>
```

## Push Notification SDK

```swift
import PushNotificationIOSSDK

PushNotifications.shared.initialize(
    pushConfig: PushConfig(pushApiBaseUrl: URL(string: "http://localhost:8082/api/v1")!)
)

PushNotifications.shared.setUserId("user_123")
try await PushNotifications.shared.registerDevice(pushToken: fcmToken)
```

The push SDK does not force a Firebase dependency. In native iOS, get the FCM token from Firebase Messaging and pass it to `registerDevice(pushToken:)`. In Flutter, use `firebase_messaging` on iOS and pass the token through the push MethodChannel bridge.

Push registration calls:

```text
POST /api/v1/devices/register
{ "userId": "...", "fcmToken": "...", "platform": "ios" }
```

## Using Both SDKs Together

If the app installs both SDKs, pass the event tracker into the push SDK to auto-track notification engagement:

```swift
PushNotifications.shared.initialize(
    pushConfig: PushConfig(pushApiBaseUrl: URL(string: "http://localhost:8082/api/v1")!),
    analyticsTracker: EventTracker.shared
)
```

Then call these from your notification delegate:

```swift
PushNotifications.shared.handleRemoteNotification(userInfo: userInfo)
PushNotifications.shared.trackNotificationOpened(userInfo: response.notification.request.content.userInfo)
```

When an analytics tracker is supplied, these become normal analytics events:

- `push_notification_received`
- `push_notification_opened`
- `push_notification_dismissed`
- `push_notification_action_clicked`

## Flutter iOS Usage

Use the bridge files as two separate Flutter-facing SDKs:

- Event tracking bridge:
  - Dart: `flutter_ios_bridge/event_tracking/tracker_ios_sdk.dart`
  - Swift plugin: `flutter_ios_bridge/event_tracking/TrackerIosSdkPlugin.swift`
  - Channel: `event_tracking_ios_sdk`

- Push notification bridge:
  - Dart: `flutter_ios_bridge/push_notifications/push_notification_ios_sdk.dart`
  - Swift plugin: `flutter_ios_bridge/push_notifications/PushNotificationIosSdkPlugin.swift`
  - Channel: `push_notification_ios_sdk`

Flutter apps can install one or both bridges. For push, get the FCM token through FlutterFire and pass it to `PushNotificationIosSdk.registerPushToken(token)`.
