package com.tracker.pushsdk.modles

data class PushNotification(
    val id: String,
    val title: String,
    val body: String,
    val imageUrl: String? = null,
    val iconUrl: String? = null,
    val deeplink: String? = null,
    val campaign: String? = null,
    val category: String? = null,
    val actions: List<NotificationAction>? = null,
    val customData: Map<String, String>? = null,
    val priority: NotificationPriority = NotificationPriority.DEFAULT,
    val sound: String? = null,
    val badgeCount: Int? = null
)

data class NotificationAction(
    val id: String,
    val title: String,
    val deeplink: String? = null,
    val icon: Int? = null
)

enum class NotificationPriority {
    MIN, LOW, DEFAULT, HIGH, MAX
}

data class NotificationEvent(
    val notificationId: String,
    val eventType: NotificationEventType,
    val campaign: String?,
    val category: String?,
    val actionId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class NotificationEventType(val value: String) {
    RECEIVED("received"),
    OPENED("opened"),
    DISMISSED("dismissed"),
    ACTION_CLICKED("action_clicked")
}

data class DeviceRegistrationPayload(
    val userId: String?,        // Changed from optional to match your DTO
    val fcmToken: String,       // Changed from pushToken to fcmToken
    val platform: String        // "android" or "ios"
)

data class NotificationEventPayload(
    val appId: String,
    val deviceId: String,
    val userId: String?,
    val notificationId: String,
    val event: String,
    val campaign: String?,
    val category: String?,
    val actionId: String?,
    val timestamp: Long
)