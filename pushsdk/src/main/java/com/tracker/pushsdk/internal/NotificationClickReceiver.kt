package com.tracker.pushsdk.internal

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tracker.pushsdk.PushNotifications
import com.tracker.pushsdk.modles.*
internal class NotificationClickReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_NOTIFICATION_OPENED = "com.tracker.push.NOTIFICATION_OPENED"
        const val ACTION_NOTIFICATION_ACTION = "com.tracker.push.NOTIFICATION_ACTION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getStringExtra("notification_id") ?: return
        val campaign = intent.getStringExtra("campaign")
        val category = intent.getStringExtra("category")
        val deeplink = intent.getStringExtra("deeplink")

        when (intent.action) {
            ACTION_NOTIFICATION_OPENED -> {
                handleNotificationOpened(context, notificationId, campaign, category, deeplink)
            }
            ACTION_NOTIFICATION_ACTION -> {
                val actionId = intent.getStringExtra("action_id")
                handleNotificationAction(
                    context,
                    notificationId,
                    actionId,
                    campaign,
                    category,
                    deeplink
                )
            }
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId.hashCode())
    }

    private fun handleNotificationOpened(
        context: Context,
        notificationId: String,
        campaign: String?,
        category: String?,
        deeplink: String?
    ) {
        val event = NotificationEvent(
            notificationId = notificationId,
            eventType = NotificationEventType.OPENED,
            campaign = campaign,
            category = category
        )
        PushNotifications.getInstance()?.trackNotificationEvent(event)

        deeplink?.let {
            DeepLinkHandler.handle(context, it)
        } ?: run {
            launchApp(context)
        }
    }

    private fun handleNotificationAction(
        context: Context,
        notificationId: String,
        actionId: String?,
        campaign: String?,
        category: String?,
        deeplink: String?
    ) {
        val event = NotificationEvent(
            notificationId = notificationId,
            eventType = NotificationEventType.ACTION_CLICKED,
            campaign = campaign,
            category = category,
            actionId = actionId
        )
        PushNotifications.getInstance()?.trackNotificationEvent(event)

        deeplink?.let {
            DeepLinkHandler.handle(context, it)
        } ?: run {
            launchApp(context)
        }
    }

    private fun launchApp(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        launchIntent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(it)
        }
    }
}
