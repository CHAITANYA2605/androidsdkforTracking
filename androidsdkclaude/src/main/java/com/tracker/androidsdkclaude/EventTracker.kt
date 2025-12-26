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
import com.tracker.androidsdkclaude.AppLifecycleTracker

class EventTracker private constructor(
    private val context: Context,
    private var config: TrackerConfig
) {
    private val eventQueue = ConcurrentLinkedQueue<Event>()
    private val storage = EventStorage(context)
    private val deviceInfoProvider = DeviceInfoProvider(context)
    private val apiClient = ApiClient(config.appId)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

    private var isFlushScheduled = false
    private var userId: String? = null // NEW

    private val deviceId: String = deviceInfoProvider.getDeviceId(storage)
    private val deviceInfo: DeviceInfo = deviceInfoProvider.getDeviceInfo()

    init {
        userId = storage.getUserId()           // NEW Load user id
        loadPersistedEvents()
        scheduleFlush()

        track("app_opened")                    // auto event
    }

    companion object {
        @Volatile private var instance: EventTracker? = null

        fun initialize(context: Context, config: TrackerConfig): EventTracker =
            instance ?: synchronized(this) {
                instance ?: EventTracker(context.applicationContext, config).also { instance = it }
            }

        fun getInstance(): EventTracker =
            instance ?: throw IllegalStateException("Call initialize() first.")
    }

    fun setUserId(id: String) {
        userId = id
        storage.saveUserId(id)
        config = config.copy(userId = id)
        flush()                                // send stored events after userid assigned
    }

    fun track(eventName: String, properties: Map<String, Any> = emptyMap()) {
        eventQueue.offer(Event(eventName, properties))
        persistEvents()

        if (eventQueue.size >= config.maxBatchSize) flush()
    }

    fun flush() { scope.launch { flushEvents() } }

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
        if (userId == null) {                   // NEW - hold events until userId exists
            persistEvents()
            return
        }

        if (eventQueue.isEmpty()) return

        val eventsToSend = mutableListOf<Event>()
        val batchSize = minOf(eventQueue.size, config.maxBatchSize)

        repeat(batchSize) { eventQueue.poll()?.let(eventsToSend::add) }
        if (eventsToSend.isEmpty()) return

        var retries = 0
        var success = false

        while (!success) {
            try {
                sendEventsToApi(eventsToSend)
                success = true
                persistEvents()
            } catch (e: Exception) {
                retries++
                delay(minOf(config.retryDelayMs * (1 shl retries), 300000L)) // backoff max 5min
            }
        }
    }

    private suspend fun sendEventsToApi(events: List<Event>) {
        val payload = ApiPayload(
            userid = userId,
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
        storage.loadEvents().forEach(eventQueue::offer)
    }
}
