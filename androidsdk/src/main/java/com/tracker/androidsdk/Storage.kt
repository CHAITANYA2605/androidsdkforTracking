package com.tracker.androidsdk

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

object Storage {
    private lateinit var prefs: SharedPreferences
    fun init(context: Context) {
        prefs = context.getSharedPreferences("sdk_events", Context.MODE_PRIVATE)
    }

    fun save(event: Map<String, Any>) {
        val queue = prefs.getStringSet("queue", mutableSetOf()) ?: mutableSetOf()
        queue.add(JSONObject(event).toString())
        prefs.edit().putStringSet("queue", queue).apply()
    }

    fun fetch(): List<String> = prefs.getStringSet("queue", emptySet())!!.toList()

    fun clear() = prefs.edit().remove("queue").apply()
}
