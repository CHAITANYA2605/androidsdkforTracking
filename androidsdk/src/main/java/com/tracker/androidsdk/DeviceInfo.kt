package com.tracker.androidsdk

import android.os.Build
import java.util.*

object DeviceInfo {
    fun collect(): Map<String, Any> {
        return mapOf(
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "os_version" to Build.VERSION.RELEASE,
            "sdk_int" to Build.VERSION.SDK_INT,
            "locale" to Locale.getDefault().toString(),
            "timezone" to TimeZone.getDefault().id
        )
    }
}
