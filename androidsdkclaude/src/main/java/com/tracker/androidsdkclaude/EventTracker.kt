package com.tracker.androidsdkclaude

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.tracker.androidsdkclaude.config.TrackerConfig
import com.tracker.androidsdkclaude.model.*
import com.tracker.androidsdkclaude.Network.ApiClient
import com.tracker.androidsdkclaude.Storage.EventStorage
import com.tracker.androidsdkclaude.utils.DeviceInfoProvider
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

class EventTracker private constructor(
    private val context: Context,
    private var config: TrackerConfig
) {
    private val eventQueue = ConcurrentLinkedQueue<Event>()
    private val storage: EventStorage = EventStorage(context)
    private val deviceInfoProvider: DeviceInfoProvider = DeviceInfoProvider(context)
    private val apiClient: ApiClient = ApiClient(config.appId)
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isFlushScheduled = false
    private val deviceId: String
    private val deviceInfo: DeviceInfo

    init {
        deviceId = deviceInfoProvider.getDeviceId(storage)
        deviceInfo = deviceInfoProvider.getDeviceInfo()

        loadPersistedEvents()
        scheduleFlush()
    }

    companion object {
        @Volatile
        private var instance: EventTracker? = null

        fun initialize(context: Context, config: TrackerConfig): EventTracker {
            return instance ?: synchronized(this) {
                instance ?: EventTracker(context.applicationContext, config).also {
                    instance = it
                }
            }
        }

        fun getInstance(): EventTracker {
            return instance ?: throw IllegalStateException("EventTracker not initialized. Call initialize() first.")
        }
    }

    fun setUserId(userId: String) {
        config = config.copy(userId = userId)
    }

    fun track(eventName: String, properties: Map<String, Any> = emptyMap()) {
        val event = Event(
            name = eventName,
            properties = properties
        )

        eventQueue.offer(event)
        persistEvents()

        if (eventQueue.size >= config.maxBatchSize) {
            flush()
        }
    }

    fun flush() {
        scope.launch {
            flushEvents()
        }
    }

    fun shutdown() {
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        flush()
    }

    private fun scheduleFlush() {
        if (isFlushScheduled) return

        isFlushScheduled = true
        handler.postDelayed({
            isFlushScheduled = false
            flush()
            scheduleFlush()
        }, config.flushIntervalSeconds * 1000L)
    }

    private suspend fun flushEvents() {
        if (eventQueue.isEmpty()) return

        val eventsToSend = mutableListOf<Event>()
        val batchSize = minOf(eventQueue.size, config.maxBatchSize)

        repeat(batchSize) {
            eventQueue.poll()?.let { eventsToSend.add(it) }
        }

        if (eventsToSend.isEmpty()) return

        // Infinite retries with exponential backoff
        var retries = 0
        var success = false

        while (!success) {
            try {
                sendEventsToApi(eventsToSend)
                success = true
                persistEvents()
            } catch (e: Exception) {
                retries++
                // Exponential backoff with max delay of 5 minutes
                val delayMs = minOf(config.retryDelayMs * (1 shl (retries - 1)), 300000L)
                delay(delayMs)
            }
        }
    }

    private suspend fun sendEventsToApi(events: List<Event>) {
        val payload = ApiPayload(
            userid = config.userId,
            deviceid = deviceId,
            deviceinfo = deviceInfo,
            events = events
        )

        apiClient.sendEvents(config.apiUrl, payload)
    }

    private fun persistEvents() {
        storage.saveEvents(eventQueue.toList())
    }

    private fun loadPersistedEvents() {
        val events = storage.loadEvents()
        events.forEach { eventQueue.offer(it) }
    }
}