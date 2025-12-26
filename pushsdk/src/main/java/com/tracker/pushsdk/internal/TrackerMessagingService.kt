package com.tracker.pushsdk.internal

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tracker.pushsdk.PushNotifications
import com.tracker.pushsdk.modles.*

internal class TrackerMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "PushMessagingService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val notification = parseNotification(remoteMessage)

        if (notification != null) {
            trackNotificationEvent(
                notificationId = notification.id,
                eventType = NotificationEventType.RECEIVED,
                campaign = notification.campaign,
                category = notification.category
            )

            NotificationBuilder.buildAndShow(this, notification)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PushNotifications.getInstance()?.onPushTokenRefresh(token)
    }

    private fun parseNotification(remoteMessage: RemoteMessage): PushNotification? {
        val data = remoteMessage.data

        if (data.isEmpty()) return null

        val notificationId = data["notification_id"] ?: data["id"]
        ?: System.currentTimeMillis().toString()

        val title = data["title"] ?: remoteMessage.notification?.title ?: "Notification"
        val body = data["body"] ?: remoteMessage.notification?.body ?: ""
        val imageUrl = data["image_url"]
        val deeplink = data["deeplink"] ?: data["deep_link"]
        val campaign = data["campaign"]
        val category = data["category"]

        val priority = when (data["priority"]?.lowercase()) {
            "min" -> NotificationPriority.MIN
            "low" -> NotificationPriority.LOW
            "high" -> NotificationPriority.HIGH
            "max" -> NotificationPriority.MAX
            else -> NotificationPriority.DEFAULT
        }

        return PushNotification(
            id = notificationId,
            title = title,
            body = body,
            imageUrl = imageUrl,
            deeplink = deeplink,
            campaign = campaign,
            category = category,
            priority = priority
        )
    }

    private fun trackNotificationEvent(
        notificationId: String,
        eventType: NotificationEventType,
        campaign: String?,
        category: String?
    ) {
        val event = NotificationEvent(
            notificationId = notificationId,
            eventType = eventType,
            campaign = campaign,
            category = category
        )
        PushNotifications.getInstance()?.trackNotificationEvent(event)
    }
}