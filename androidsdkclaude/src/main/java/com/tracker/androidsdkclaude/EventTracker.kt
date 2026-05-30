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
    private val storage = EventStorage(context)
    private val deviceInfoProvider = DeviceInfoProvider(context)
    private val apiClient = ApiClient(config.appId)

    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

    private var isFlushScheduled = false
    private var userId: String? = null

    private val deviceId: String = deviceInfoProvider.getDeviceId(storage)
    private val deviceInfo: DeviceInfo = deviceInfoProvider.getDeviceInfo()

    // 🔐 ONE-TIME INIT FLAG
    @Volatile
    private var initialized = false

    private val MAX_BUFFER_AGE_MS = 3L * 24 * 60 * 60 * 1000

    init {
        userId = storage.getUserId()
        initialized = storage.isInitialized()

        loadPersistedEvents()
        if (storage.isTrackingEnabled()) scheduleFlush()

        track("app_opened")
    }

    companion object {
        @Volatile private var instance: EventTracker? = null

        fun initialize(context: Context, config: TrackerConfig): EventTracker =
            instance ?: synchronized(this) {
                instance ?: EventTracker(context.applicationContext, config)
                    .also { instance = it }
            }

        fun getInstance(): EventTracker =
            instance ?: throw IllegalStateException("Call initialize() first.")
    }

    fun setUserId(id: String) {
        userId = id
        storage.saveUserId(id)
        config = config.copy(userId = id)
        flush()
    }

    fun enableTracking() {
        storage.setTrackingEnabled(true)
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scheduleFlush()
    }

    fun disableTracking() {
        storage.setTrackingEnabled(false)
        storage.purgeBufferedOlderThan(MAX_BUFFER_AGE_MS)
        handler.removeCallbacksAndMessages(null)
        scope.coroutineContext[Job]?.cancelChildren()
    }

    fun track(eventName: String, properties: Map<String, Any> = emptyMap()) {
        val ev = Event(eventName, properties)

        if (!storage.isTrackingEnabled()) {
            storage.appendBufferedEvent(ev)
            return
        }

        eventQueue.offer(ev)
        persistEvents()

        if (eventQueue.size >= config.maxBatchSize) flush()
    }

    fun flush() {
        scope.launch { flushEvents() }
    }

    private suspend fun flushEvents() {
        if (userId == null) {
            persistEvents()
            return
        }

        // 🔐 INIT — RUN ONLY ONCE EVER
        if (!initialized) {
            val ok = runInitOnce()
            if (!ok) return
        }

        if (!storage.isTrackingEnabled()) return
        if (eventQueue.isEmpty()) return

        val batch = mutableListOf<Event>()
        repeat(minOf(eventQueue.size, config.maxBatchSize)) {
            eventQueue.poll()?.let(batch::add)
        }

        if (batch.isEmpty()) return

        var retries = 0
        while (true) {
            try {
                sendEvents(batch)
                persistEvents()
                return
            } catch (_: Exception) {
                retries++
                delay(minOf(config.retryDelayMs * (1 shl retries), 300_000))
            }
        }
    }

    /**
     * 🔐 ONE-TIME HANDSHAKE
     */
    private suspend fun runInitOnce(): Boolean {
        return try {
            val status = apiClient.checkInit(config.apiUrl, deviceId)
            if (status in 200..299) {
                initialized = true
                storage.setInitialized(true)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun sendEvents(events: List<Event>) {
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

    private fun scheduleFlush() {
        if (isFlushScheduled || !storage.isTrackingEnabled()) return
        isFlushScheduled = true

        handler.postDelayed({
            isFlushScheduled = false
            flush()
            scheduleFlush()
        }, config.flushIntervalSeconds * 1000L)
    }
}
