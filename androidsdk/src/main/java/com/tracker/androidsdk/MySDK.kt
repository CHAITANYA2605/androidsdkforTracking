package com.tracker.androidsdk

import android.content.Context

object MySDK {
    private lateinit var appId: String
    fun init(context: Context, appId: String) {
        this.appId = appId
        Storage.init(context)
        RetryWorker.schedule(context)
    }

    fun trackEvent(name: String, props: Map<String, Any>) {
        val event = mapOf(
            "app_id" to appId,
            "event" to name,
            "properties" to props,
            "device" to DeviceInfo.collect(),
            "timestamp" to System.currentTimeMillis()
        )
        Storage.save(event)
    }

    fun getAppId(): String = appId
}
