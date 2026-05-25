package com.tracker.androidsdk

import android.content.Context
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object MySDK {
    private lateinit var appId: String
    private lateinit var serverApiKey: String
    private var userId: String? = null
    private var deviceId: String = ""

    fun init(context: Context, appId: String, serverApiKey: String = appId) {
        this.appId = appId
        this.serverApiKey = serverApiKey
        this.deviceId = Settings.Secure.getString(
            context.applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "android_${System.currentTimeMillis()}"
        Storage.init(context)
        RetryWorker.schedule(context)
    }

    fun setUserId(userId: String) {
        this.userId = userId
    }

    fun trackEvent(name: String, props: Map<String, Any>) {
        val event = mapOf(
            "app_id" to appId,
            "event" to name,
            "properties" to props,
            "device" to DeviceInfo.collect(),
            "timestamp" to System.currentTimeMillis(),
            "occurredAt" to isoNow()
        )
        Storage.save(event)
    }

    fun getAppId(): String = appId
    fun getServerApiKey(): String = serverApiKey
    fun getUserId(): String? = userId
    fun getDeviceId(): String = deviceId

    private fun isoNow(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }
}
