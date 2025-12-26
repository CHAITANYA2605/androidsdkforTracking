package com.tracker.pushsdk.internal


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.content.*
import com.tracker.pushsdk.modles.DeviceInfo

internal class DeviceInfoProvider(private val context: Context) {

    fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            platform = "Android",
            osVersion = Build.VERSION.RELEASE,
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            sdkVersion = Build.VERSION.SDK_INT,
            isWifi = isWifiConnected(),
            simCompany = getSimOperatorName()
        )
    }

    private fun getSimOperatorName(): String {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            return "Permission Denied"
        }
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return telephonyManager.simOperatorName ?: "Unknown"
    }

    fun getDeviceId(storage: PushStorage): String {
        var deviceId = storage.loadDeviceId()
        if (deviceId == null) {
            deviceId = try {
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                ) ?: "dev_${System.currentTimeMillis()}"
            } catch (e: Exception) {
                "dev_${System.currentTimeMillis()}"
            }
            storage.saveDeviceId(deviceId)
        }
        return deviceId
    }

    private fun isWifiConnected(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo?.type == ConnectivityManager.TYPE_WIFI
        }
    }
}
