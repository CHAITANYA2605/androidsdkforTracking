package com.tracker.androidsdkclaude.Network

import com.google.gson.Gson
import com.tracker.androidsdkclaude.model.ApiPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ApiClient(private val appId: String) {
    private val gson = Gson()
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Sends events payload and returns the HTTP status code.
     * Caller handles special codes (e.g. 300 → disable).
     */
    suspend fun sendEvents(apiUrl: String, payload: ApiPayload): Int = withContext(Dispatchers.IO) {
        val json = gson.toJson(payload)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(apiUrl)
            .post(body)
            .addHeader("Authorization", "Bearer $appId")
            .addHeader("X-App-ID", appId)
            .addHeader("Content-Type", "application/json")
            .build()

        val response = okHttpClient.newCall(request).execute()
        val code = response.code
        response.body?.close()
        response.close()
        code
    }

    /**
     * Init/status check using the same ingest endpoint but with query parameter ?check=init
     * so the server can route inside the same controller. If apiUrl already contains a query
     * parameter, append with & instead of ?.
     */
    suspend fun checkInit(apiUrl: String, deviceId: String): Int = withContext(Dispatchers.IO) {
        val initUrl = if (apiUrl.contains("?")) "${apiUrl}&check=init" else "${apiUrl}?check=init"

        val payload = mapOf("deviceid" to deviceId, "app" to appId)
        val json = gson.toJson(payload)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(initUrl)
            .post(body)
            .addHeader("Authorization", "Bearer $appId")
            .addHeader("X-App-ID", appId)
            .addHeader("Content-Type", "application/json")
            .build()

        val response = okHttpClient.newCall(request).execute()
        val code = response.code
        response.body?.close()
        response.close()
        code
    }
}
