package com.tracker.pushsdk


import android.content.Context
import com.tracker.pushsdk.config.PushConfig
import com.tracker.pushsdk.config.PushRegistrationState
import com.tracker.pushsdk.internal.*
import com.tracker.pushsdk.modles.*

class PushNotifications private constructor() {

    companion object {
        @Volatile
        private var instance: PushNotifications? = null

        fun getInstance(): PushNotifications? = instance

        fun initialize(context: Context, config: PushConfig) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = PushNotifications().apply {
                            init(context.applicationContext, config)
                        }
                    }
                }
            }
        }
    }

    private lateinit var config: PushConfig
    private lateinit var pushManager: PushManager
    private var currentUserId: String? = null

    private fun init(context: Context, config: PushConfig) {
        this.config = config
        this.currentUserId = config.userId

        val storage = PushStorage(context)
        val deviceInfoProvider = DeviceInfoProvider(context)

        pushManager = PushManager(
            context = context,
            config = config,
            storage = storage,
            deviceInfoProvider = deviceInfoProvider
        )

        pushManager.initialize(config.userId)
    }

    /**
     * Set user ID for push notifications
     * This will trigger registration if it was pending
     */
    fun setUserId(userId: String) {
        val wasNull = currentUserId == null
        currentUserId = userId

        if (wasNull) {
            pushManager.onUserIdAvailable(userId)
        }
    }

    /**
     * Clear user ID (on logout)
     */
    fun clearUserId() {
        currentUserId = null
        pushManager.onUserLogout()
    }

    /**
     * Get current registration state
     */
    fun getRegistrationState(): PushRegistrationState {
        return pushManager.state
    }

    /**
     * Handle token refresh (internal use)
     */
    internal fun onPushTokenRefresh(newToken: String) {
        pushManager.onTokenRefresh(newToken)
    }

    /**
     * Track notification event (internal use)
     */
    internal fun trackNotificationEvent(event: NotificationEvent) {
        pushManager.trackNotificationEvent(event)
    }

    /**
     * Get push config (internal use)
     */
    internal fun getPushConfig(): PushConfig = config
}
