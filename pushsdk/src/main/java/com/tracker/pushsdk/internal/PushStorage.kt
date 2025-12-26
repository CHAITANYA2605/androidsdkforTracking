package com.tracker.pushsdk.internal

import android.content.Context
import android.content.SharedPreferences

internal class PushStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("PushSDKPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_REGISTRATION_STATE = "registration_state"
    }

    fun saveFCMToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun loadFCMToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    fun saveDeviceId(deviceId: String) {
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }

    fun loadDeviceId(): String? {
        return prefs.getString(KEY_DEVICE_ID, null)
    }

    fun saveRegistrationState(state: String) {
        prefs.edit().putString(KEY_REGISTRATION_STATE, state).apply()
    }

    fun loadRegistrationState(): String? {
        return prefs.getString(KEY_REGISTRATION_STATE, null)
    }
}
