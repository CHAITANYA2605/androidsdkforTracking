package com.tracker.androidsdkclaude.Storage

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tracker.androidsdkclaude.model.Event

class EventStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "EventTrackerPrefs"
        private const val KEY_EVENTS = "persisted_events"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_USER_ID = "user_id"
    }

    fun saveEvents(events: List<Event>) {
        try {
            val json = gson.toJson(events)
            prefs.edit().putString(KEY_EVENTS, json).apply()
        } catch (e: Exception) {
            // Log error
        }
    }

    fun loadEvents(): List<Event> {
        try {
            val json = prefs.getString(KEY_EVENTS, null) ?: return emptyList()
            val type = object : TypeToken<List<Event>>() {}.type
            return gson.fromJson(json, type)
        } catch (e: Exception) {
            return emptyList()
        }
    }

    fun saveDeviceId(deviceId: String) {
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }

    fun loadDeviceId(): String? {
        return prefs.getString(KEY_DEVICE_ID, null)
    }
    fun saveUserId(id: String) {
        prefs.edit().putString(KEY_USER_ID, id).apply()
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
}