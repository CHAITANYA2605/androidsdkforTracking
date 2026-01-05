package com.tracker.androidsdkclaude.Storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tracker.androidsdkclaude.model.Event

private const val PREFS_NAME = "tracker_storage"
private const val KEY_EVENTS = "events"
private const val KEY_DEVICE_ID = "device_id"
private const val KEY_USER_ID = "user_id"
private const val KEY_TRACKING_ENABLED = "tracking_enabled"
private const val KEY_BUFFERED_EVENTS = "buffered_events"

class EventStorage(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveEvents(events: List<Event>) {
        val json = gson.toJson(events)
        prefs.edit().putString(KEY_EVENTS, json).apply()
    }

    fun loadEvents(): List<Event> {
        val json = prefs.getString(KEY_EVENTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Event>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
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

    // Persisted tracking enabled flag (default true)
    fun isTrackingEnabled(): Boolean = prefs.getBoolean(KEY_TRACKING_ENABLED, true)
    fun setTrackingEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_TRACKING_ENABLED, enabled).apply()

    // Buffered events: used while tracking is disabled to avoid losing new events
    fun saveBufferedEvents(events: List<Event>) {
        val json = gson.toJson(events)
        prefs.edit().putString(KEY_BUFFERED_EVENTS, json).apply()
    }

    fun appendBufferedEvent(event: Event) {
        val current = loadBufferedEvents().toMutableList()
        current.add(event)
        saveBufferedEvents(current)
    }

    fun loadBufferedEvents(): List<Event> {
        val json = prefs.getString(KEY_BUFFERED_EVENTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Event>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearBufferedEvents() {
        prefs.edit().remove(KEY_BUFFERED_EVENTS).apply()
    }

    /**
     * Return buffered events newer than the provided cutoff (ms).
     * Useful when re-enabling: only merge recent buffered events.
     */
    fun loadBufferedEventsNewerThan(cutoffMs: Long): List<Event> {
        val now = System.currentTimeMillis()
        return loadBufferedEvents().filter { now - it.timestamp <= cutoffMs }
    }

    /**
     * Remove buffered events older than cutoffMs (ms).
     * Called when tracking is disabled so we don't keep indefinite data while stopped.
     */
    fun purgeBufferedOlderThan(cutoffMs: Long) {
        val now = System.currentTimeMillis()
        val recent = loadBufferedEvents().filter { now - it.timestamp <= cutoffMs }
        saveBufferedEvents(recent)
    }
}
