package com.tracker.pushsdk.config

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

data class PushConfig(
    val apiUrl: String = "http://10.0.2.2:8082/api/v1/",
    val appId: String,
    val userId: String? = null,
    val enabled: Boolean = true,
    val requireUserId: Boolean = true,
    val autoTrackEvents: Boolean = true,
    @DrawableRes val defaultNotificationIcon: Int? = null,
    @ColorInt val defaultNotificationColor: Int? = null,
    val notificationChannelId: String = "push_notifications",
    val notificationChannelName: String = "Notifications",
    val notificationChannelDescription: String = "Push notifications",
    val showNotificationsWhenAppInForeground: Boolean = true,
    val deepLinkHandler: ((String) -> Unit)? = null,
    val deviceRegistrationUrl: String? = null,
    val notificationEventsUrl: String? = null
)

enum class PushRegistrationState {
    NOT_INITIALIZED,
    PENDING_USER,
    REGISTERED,
    FAILED
}