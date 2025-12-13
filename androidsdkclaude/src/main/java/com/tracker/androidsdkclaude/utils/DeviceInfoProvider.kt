package com.tracker.androidsdkclaude.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.provider.Settings
import com.tracker.androidsdkclaude.Storage.EventStorage
import com.tracker.androidsdkclaude.model.DeviceInfo
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

class DeviceInfoProvider(private val context: Context) {

    fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            platform = "Android",
            os_version = Build.VERSION.RELEASE,
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            sdk_version = Build.VERSION.SDK_INT,
            iswifi = isWifiConnected(context),
            Simcompany = getSimOperatorName(context),
        )
    }

    private fun getSimOperatorName(context: Context): String {
        // Check for runtime permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

            return "Permission Denied"
        }

        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        // This returns the Service Provider Name (SPN) (e.g., "Verizon", "T-Mobile")
        return telephonyManager.simOperatorName ?: "Unknown"

    }

    fun getDeviceId(storage: EventStorage): String {
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
    @SuppressLint("ServiceCast")
    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Starting from API 23 (Marshmallow), use Network and NetworkCapabilities
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            // Deprecated approach for older APIs (API < 23)
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.getNetworkInfo(connectivityManager.activeNetwork)
            @Suppress("DEPRECATION")
            return networkInfo != null
        }
    }
}