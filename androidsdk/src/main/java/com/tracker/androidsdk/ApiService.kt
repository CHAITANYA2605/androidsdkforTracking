package com.tracker.androidsdk

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.util.concurrent.TimeUnit

object ApiService {
    private const val API_URL = "http://localhost:8080/api/v1/ingest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(350, TimeUnit.SECONDS)
        .writeTimeout(350, TimeUnit.SECONDS)
        .readTimeout(530, TimeUnit.SECONDS)
        .build()

    fun send(events: List<String>): Boolean {
        if (events.isEmpty()) return true

        return try {
            val jsonArray = JSONArray(events)
            val body = jsonArray.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val isSuccess = response.isSuccessful

            println("Sent ${events.size} events to $API_URL - Status: ${response.code}")
            response.close()

            isSuccess
        } catch (e: Exception) {
            println("Failed to send events: ${e.message}")
            false
        }
    }
}