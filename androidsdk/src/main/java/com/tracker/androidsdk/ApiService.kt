package com.tracker.androidsdk

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiService {
    private const val API_URL = "http://10.0.2.2:8080/api/v1/events/track"

    private val client = OkHttpClient.Builder()
        .connectTimeout(350, TimeUnit.SECONDS)
        .writeTimeout(350, TimeUnit.SECONDS)
        .readTimeout(530, TimeUnit.SECONDS)
        .build()

    fun send(events: List<String>): Boolean {
        if (events.isEmpty()) return true
        val userId = MySDK.getUserId() ?: return false

        return try {
            val eventItems = JSONArray()
            events.forEach { rawEvent ->
                val saved = JSONObject(rawEvent)
                val item = JSONObject()
                    .put("name", saved.optString("event"))
                    .put("properties", saved.optJSONObject("properties") ?: JSONObject())
                    .put("occurredAt", saved.optString("occurredAt"))
                eventItems.put(item)
            }

            val payload = JSONObject()
                .put("userId", userId)
                .put("deviceId", MySDK.getDeviceId())
                .put("deviceInfo", JSONObject(DeviceInfo.collect()))
                .put("events", eventItems)

            val body = payload.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer ${MySDK.getServerApiKey()}")
                .addHeader("X-App-ID", MySDK.getAppId())
                .build()

            val response = client.newCall(request).execute()
            val isSuccess = response.isSuccessful

            println("Sent ${events.size} events to main backend $API_URL - Status: ${response.code}")
            response.close()

            isSuccess
        } catch (e: Exception) {
            println("Failed to send events: ${e.message}")
            false
        }
    }
}
