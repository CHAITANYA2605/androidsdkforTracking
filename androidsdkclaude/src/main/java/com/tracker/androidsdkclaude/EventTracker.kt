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

    // Make scope mutable so we can recreate it on re-enable
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

    private var isFlushScheduled = false
    private var userId: String? = null
    private val deviceId: String = deviceInfoProvider.getDeviceId(storage)
    private val deviceInfo: DeviceInfo = deviceInfoProvider.getDeviceInfo()
    private var recheckJob: Job? = null
    private var recheckScope: CoroutineScope? = null

    // Buffer age limit applied only while tracking is disabled
    private val MAX_BUFFER_AGE_MS: Long = 3L * 24 * 60 * 60 * 1000 // 3 days

    init {
        userId = storage.getUserId()
        loadPersistedEvents()
        // Only run flush scheduling when tracking is enabled
        if (storage.isTrackingEnabled()) scheduleFlush()
        track("app_opened")
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
        flush()
    }

    // Optional public controls
    fun enableTracking() {
        storage.setTrackingEnabled(true)
        // stop recheck loop if any
        recheckJob?.cancel()
        recheckScope?.cancel()
        recheckJob = null
        recheckScope = null

        // recreate main scope for background work
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        // merge buffered events into main queue — include only recent buffered events (if any)
        val buffered = storage.loadBufferedEventsNewerThan(MAX_BUFFER_AGE_MS)
        if (buffered.isNotEmpty()) {
            buffered.forEach { eventQueue.offer(it) }
            storage.clearBufferedEvents()
            persistEvents()
        }

        scheduleFlush()
    }

    fun disableTracking() {
        storage.setTrackingEnabled(false)
        // Do not clear persisted main queue; keep events persisted.
        // Purge buffered events older than 3 days immediately (requirement)
        storage.purgeBufferedOlderThan(MAX_BUFFER_AGE_MS)

        // Stop scheduling and cancel background work, but keep events persisted.
        handler.removeCallbacksAndMessages(null)
        scope.coroutineContext[Job]?.cancelChildren()
        // start recheck loop to attempt re-enable automatically — this loop will also purge old buffered events periodically
        startRecheckLoop()
    }

    /**
     * Public API to trigger a manual re-check with the backend immediately.
     * Returns true if re-enabled as a result of this call.
     */
    suspend fun recheckServerNow(): Boolean = withContext(Dispatchers.IO) {
        val status = try {
            apiClient.checkInit(config.apiUrl, deviceId)
        } catch (e: Exception) {
            -1
        }
        if (status in 200..299) {
            withContext(Dispatchers.Main) {
                enableTracking()
            }
            true
        } else {
            false
        }
    }

    fun track(eventName: String, properties: Map<String, Any> = emptyMap()) {
        val ev = Event(eventName, properties)

        // If tracking is disabled, store the event in the buffered events so it is not lost.
        // Buffered events will be purged after 3 days while tracking is disabled.
        if (!storage.isTrackingEnabled()) {
            storage.appendBufferedEvent(ev)
            return
        }

        eventQueue.offer(ev)
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
        if (!storage.isTrackingEnabled()) return
        isFlushScheduled = true

        handler.postDelayed({
            isFlushScheduled = false
            flush()
            scheduleFlush()
        }, config.flushIntervalSeconds * 1000L)
    }

    private suspend fun flushEvents() {
        if (userId == null) {
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
            } catch (e: DisabledByServerException) {
                // Server asked us to stop — stop retrying and exit
                return
            } catch (e: Exception) {
                retries++
                delay(minOf(config.retryDelayMs * (1 shl retries), 300000L))
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

        val status = apiClient.sendEvents(config.apiUrl, payload)

        if (status == 300) {
            // Server instructs SDK to disable itself. Do NOT clear main queue/persisted events.
            storage.setTrackingEnabled(false)

            // Purge buffered events older than 3 days immediately
            storage.purgeBufferedOlderThan(MAX_BUFFER_AGE_MS)

            // Stop scheduled flushes and background sends, but keep events persisted.
            handler.removeCallbacksAndMessages(null)
            scope.coroutineContext[Job]?.cancelChildren()
            // start a recheck loop that polls the server for a new status (and also purges old buffered events periodically)
            startRecheckLoop()
            throw DisabledByServerException()
        }

        if (status !in 200..299) {
            throw Exception("Failed to send events, status=$status")
        }
    }

    private fun startRecheckLoop() {
        // If already running, keep running
        if (recheckJob?.isActive == true) return

        // create a dedicated scope for rechecks so it isn't affected by the main scope cancellation
        recheckScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        recheckJob = recheckScope!!.launch {
            while (isActive) {
                try {
                    // purge old buffered events on each iteration (so we don't keep >3 days while disabled)
                    storage.purgeBufferedOlderThan(MAX_BUFFER_AGE_MS)

                    val status = try {
                        apiClient.checkInit(config.apiUrl, deviceId)
                    } catch (e: Exception) {
                        -1
                    }

                    if (status in 200..299) {
                        // On success, re-enable (on main thread)
                        withContext(Dispatchers.Main) {
                            enableTracking()
                        }
                        break
                    }

                    // wait then retry
                    delay(config.recheckIntervalSeconds * 1000L)
                } catch (e: CancellationException) {
                    break
                } catch (_: Exception) {
                    delay(config.recheckIntervalSeconds * 1000L)
                }
            }
        }
    }

    private fun persistEvents() {
        storage.saveEvents(eventQueue.toList())
    }

    private fun loadPersistedEvents() {
        // load persisted main queue events (no age-based deletion while enabled)
        storage.loadEvents().forEach(eventQueue::offer)
        // on startup, buffered events are left in storage; only merged when tracking is enabled
    }

    private class DisabledByServerException : Exception()
}
