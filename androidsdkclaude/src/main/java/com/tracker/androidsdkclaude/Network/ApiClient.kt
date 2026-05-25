package com.tracker.androidsdkclaude.Network

import com.google.gson.Gson
import com.tracker.androidsdkclaude.model.ApiPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiClient(
    private val appId: String,
    private val serverApiKey: String
) {
    private val gson = Gson()
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun sendEvents(apiUrl: String, payload: ApiPayload) {
        val json = gson.toJson(payload)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(apiUrl)
            .post(body)
            .addHeader("Authorization", "Bearer $serverApiKey")
            .addHeader("X-App-ID", appId)
            .build()

        withContext(Dispatchers.IO) {
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw IOException("Unexpected response code: ${response.code}")
            }
        }
    }
}
