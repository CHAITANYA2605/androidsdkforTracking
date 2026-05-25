package com.tracker.androidsdkclaude.config

data class TrackerConfig(
    val appId: String,
    val serverApiKey: String = appId,
    val apiUrl: String = "http://10.0.2.2:8080/api/v1/events/track",
    val userId: String? = null,
    val maxBatchSize: Int = 100,
    val flushIntervalSeconds: Int = 30,
    val retryDelayMs: Long = 2000
)
