package com.tracker.pushsdk.internal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.tracker.pushsdk.PushNotifications

internal object DeepLinkHandler {

    private const val TAG = "DeepLinkHandler"

    fun handle(context: Context, deeplink: String) {
        Log.d(TAG, "Handling deep link: $deeplink")

        try {
            val customHandler = PushNotifications.getInstance()?.getPushConfig()?.deepLinkHandler
            if (customHandler != null) {
                customHandler.invoke(deeplink)
                return
            }

            val uri = Uri.parse(deeplink)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Log.w(TAG, "No activity found to handle deep link")
                launchMainApp(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle deep link", e)
            launchMainApp(context)
        }
    }

    private fun launchMainApp(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        launchIntent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(it)
        }
    }
}
