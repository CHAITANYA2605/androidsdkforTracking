package com.tracker.pushsdk.internal
import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.tracker.pushsdk.config.PushConfig
import com.tracker.pushsdk.config.PushRegistrationState
import com.tracker.pushsdk.modles.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

internal class PushManager(
    private val context: Context,
    private val config: PushConfig,
    private val storage: PushStorage,
    private val deviceInfoProvider: DeviceInfoProvider
) {
    companion object {
        private const val TAG = "PushManager"
    }

    private val gson = Gson()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    var state: PushRegistrationState = PushRegistrationState.NOT_INITIALIZED
        private set

    private var cachedFCMToken: String? = null
    private var currentUserId: String? = null

    init {
        loadSavedState()
    }

    fun initialize(userId: String?) {
        if (!config.enabled) {
            Log.d(TAG, "Push notifications disabled")
            return
        }

        Log.d(TAG, "Initializing push notifications for userId: $userId")
        currentUserId = userId

        coroutineScope.launch {
            try {
                val token = getFCMTokenAsync()
                if (token != null) {
                    Log.d(TAG, "✅ FCM Token retrieved: ${token.take(20)}...")
                    cachedFCMToken = token
                    storage.saveFCMToken(token)
                    subscribeToTopic("all")
                    attemptRegistration(userId, token)
                } else {
                    Log.e(TAG, "❌ Failed to get FCM token - token is null")
                    state = PushRegistrationState.FAILED
                    saveState()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception while getting FCM token", e)
                state = PushRegistrationState.FAILED
                saveState()
            }
        }
    }

    fun onUserIdAvailable(userId: String) {
        Log.d(TAG, "UserId became available: $userId")
        currentUserId = userId

        if (state == PushRegistrationState.PENDING_USER) {
            cachedFCMToken?.let { token ->
                Log.d(TAG, "UserId now available, registering push...")
                registerWithBackend(userId, token)
            } ?: run {
                Log.w(TAG, "No cached FCM token available, re-initializing...")
                initialize(userId)
            }
        } else if (state == PushRegistrationState.REGISTERED) {
            cachedFCMToken?.let { token ->
                Log.d(TAG, "Updating registration with new userId...")
                registerWithBackend(userId, token)
            }
        }
    }

    fun onUserLogout() {
        Log.d(TAG, "User logged out")
        currentUserId = null
        if (config.requireUserId) {
            state = PushRegistrationState.PENDING_USER
            saveState()
        }
    }

    fun onTokenRefresh(newToken: String) {
        Log.d(TAG, "FCM token refreshed: ${newToken.take(20)}...")
        cachedFCMToken = newToken
        storage.saveFCMToken(newToken)

        if (state == PushRegistrationState.REGISTERED && currentUserId != null) {
            Log.d(TAG, "Re-registering with new token")
            registerWithBackend(currentUserId!!, newToken)
        }
    }

    fun trackNotificationEvent(event: NotificationEvent) {
        if (!config.autoTrackEvents) return

        coroutineScope.launch {
            try {
                val deviceId = deviceInfoProvider.getDeviceId(storage)
                val url = config.notificationEventsUrl
                    ?: "${config.apiUrl}notifications/events"

                val payload = NotificationEventPayload(
                    appId = config.appId,
                    deviceId = deviceId,
                    userId = currentUserId,
                    notificationId = event.notificationId,
                    event = event.eventType.value,
                    campaign = event.campaign,
                    category = event.category,
                    actionId = event.actionId,
                    timestamp = event.timestamp
                )

                sendToBackend(url, payload)
                Log.d(TAG, "✅ Notification event tracked: ${event.eventType.value}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to track notification event", e)
            }
        }
    }

    private fun attemptRegistration(userId: String?, token: String) {
        Log.d(TAG, "Attempting registration - userId: ${userId ?: "null"}, requireUserId: ${config.requireUserId}")

        if (userId != null) {
            Log.d(TAG, "✅ UserId available, registering with backend...")
            registerWithBackend(userId, token)
        } else if (!config.requireUserId) {
            Log.d(TAG, "✅ UserId not required, registering device only...")
            registerWithBackend(null, token)
        } else {
            Log.d(TAG, "⏳ Waiting for userId before registration...")
            state = PushRegistrationState.PENDING_USER
            saveState()
        }
    }

    private fun registerWithBackend(userId: String?, token: String) {
        coroutineScope.launch {
            try {
                // Updated to match your DTO structure
                val url = config.deviceRegistrationUrl
                    ?: "${config.apiUrl}devices/register"

                Log.d(TAG, "Registering device with backend:")
                Log.d(TAG, "  - URL: $url")
                Log.d(TAG, "  - UserId: ${userId ?: "null"}")
                Log.d(TAG, "  - FCM Token: ${token.take(20)}...")
                Log.d(TAG, "  - Platform: android")

                // Simplified payload matching your DTO
                val payload = DeviceRegistrationPayload(
                    userId = userId,
                    fcmToken = token,
                    platform = "android"
                )

                sendToBackend(url, payload)

                state = PushRegistrationState.REGISTERED
                saveState()
                Log.d(TAG, "✅ Push registration successful!")
            } catch (e: Exception) {
                state = PushRegistrationState.FAILED
                saveState()
                Log.e(TAG, "❌ Failed to register push", e)
            }
        }
    }

    private suspend fun sendToBackend(url: String, payload: Any) {
        val json = gson.toJson(payload)
        Log.d(TAG, "Sending to backend: $json")

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            Log.e(TAG, "Backend error: ${response.code} - $errorBody")
            throw Exception("HTTP ${response.code}: ${response.message}")
        } else {
            val responseBody = response.body?.string()
            Log.d(TAG, "✅ Backend response: ${response.code} - $responseBody")
        }
    }

    private suspend fun getFCMTokenAsync(): String? {
        return try {
            Log.d(TAG, "Requesting FCM token from Firebase...")
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "✅ Firebase returned token: ${token?.take(20)}...")
            token
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get FCM token from Firebase", e)
            null
        }
    }

    private fun loadSavedState() {
        cachedFCMToken = storage.loadFCMToken()

        if (cachedFCMToken != null) {
            Log.d(TAG, "Loaded cached FCM token: ${cachedFCMToken!!.take(20)}...")
        }

        val savedState = storage.loadRegistrationState()
        state = if (savedState != null) {
            try {
                PushRegistrationState.valueOf(savedState)
            } catch (e: Exception) {
                PushRegistrationState.NOT_INITIALIZED
            }
        } else {
            PushRegistrationState.NOT_INITIALIZED
        }

        Log.d(TAG, "Loaded saved state: $state")
    }

    private fun saveState() {
        storage.saveRegistrationState(state.name)
        Log.d(TAG, "Saved state: $state")
    }
    private suspend fun subscribeToTopic(topic: String) {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            Log.d(TAG, "✅ Subscribed to topic: $topic")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to subscribe to topic: $topic", e)
        }
    }

}