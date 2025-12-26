package com.tracker.pushsdk.internal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tracker.pushsdk.modles.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

object NotificationBuilder {

    private const val DEFAULT_CHANNEL_ID = "push_notifications"
    private const val DEFAULT_CHANNEL_NAME = "Notifications"

    fun buildAndShow(context: Context, notification: PushNotification) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context, notificationManager)
        }

        CoroutineScope(Dispatchers.Main).launch {
            val builder = buildNotification(context, notification)
            val notificationId = notification.id.hashCode()
            notificationManager.notify(notificationId, builder.build())
        }
    }

    private suspend fun buildNotification(
        context: Context,
        notification: PushNotification
    ): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setAutoCancel(true)
            .setPriority(mapPriority(notification.priority))

        val iconRes = getNotificationIcon(context)
        builder.setSmallIcon(iconRes)

        getNotificationColor(context)?.let { color ->
            builder.setColor(color)
        }

        notification.imageUrl?.let { imageUrl ->
            val bitmap = downloadImage(imageUrl)
            bitmap?.let {
                builder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(it)
                        .bigLargeIcon(null as Bitmap?)
                )
                builder.setLargeIcon(it)
            }
        }

        if (notification.imageUrl == null) {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notification.body)
            )
        }

        val contentIntent = createContentIntent(context, notification)
        builder.setContentIntent(contentIntent)

        notification.actions?.forEach { action ->
            val actionIntent = createActionIntent(
                context,
                notification,
                action.id,
                action.deeplink
            )
            builder.addAction(0, action.title, actionIntent)
        }

        return builder
    }

    private fun createNotificationChannel(
        context: Context,
        notificationManager: NotificationManager
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = notificationManager.getNotificationChannel(DEFAULT_CHANNEL_ID)

            if (existingChannel == null) {
                val channel = NotificationChannel(
                    DEFAULT_CHANNEL_ID,
                    DEFAULT_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Push notifications"
                    enableLights(true)
                    enableVibration(true)
                }

                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun createContentIntent(
        context: Context,
        notification: PushNotification
    ): PendingIntent {
        val intent = Intent(context, NotificationClickReceiver::class.java).apply {
            action = NotificationClickReceiver.ACTION_NOTIFICATION_OPENED
            putExtra("notification_id", notification.id)
            putExtra("deeplink", notification.deeplink)
            putExtra("campaign", notification.campaign)
            putExtra("category", notification.category)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getBroadcast(
            context,
            notification.id.hashCode(),
            intent,
            flags
        )
    }

    private fun createActionIntent(
        context: Context,
        notification: PushNotification,
        actionId: String,
        deeplink: String?
    ): PendingIntent {
        val intent = Intent(context, NotificationClickReceiver::class.java).apply {
            action = NotificationClickReceiver.ACTION_NOTIFICATION_ACTION
            putExtra("notification_id", notification.id)
            putExtra("action_id", actionId)
            putExtra("deeplink", deeplink ?: notification.deeplink)
            putExtra("campaign", notification.campaign)
            putExtra("category", notification.category)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getBroadcast(
            context,
            "${notification.id}_$actionId".hashCode(),
            intent,
            flags
        )
    }

    private suspend fun downloadImage(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val url = URL(imageUrl)
            val connection = url.openConnection()
            connection.doInput = true
            connection.connect()
            val input = connection.getInputStream()
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            null
        }
    }

    private fun mapPriority(priority: NotificationPriority): Int {
        return when (priority) {
            NotificationPriority.MIN -> NotificationCompat.PRIORITY_MIN
            NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
            NotificationPriority.DEFAULT -> NotificationCompat.PRIORITY_DEFAULT
            NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            NotificationPriority.MAX -> NotificationCompat.PRIORITY_MAX
        }
    }

    private fun getNotificationIcon(context: Context): Int {
        return try {
            val resId = context.resources.getIdentifier(
                "ic_notification",
                "drawable",
                context.packageName
            )
            if (resId != 0) resId else android.R.drawable.ic_dialog_info
        } catch (e: Exception) {
            android.R.drawable.ic_dialog_info
        }
    }

    private fun getNotificationColor(context: Context): Int? {
        return try {
            val resId = context.resources.getIdentifier(
                "notification_color",
                "color",
                context.packageName
            )
            if (resId != 0) context.getColor(resId) else null
        } catch (e: Exception) {
            null
        }
    }
}
