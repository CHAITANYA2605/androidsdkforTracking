package com.tracker.androidsdkclaude.config

data class TrackerConfig(
    val apiUrl: String = "http://10.0.2.2:8080/api/v1/ingest", // Change this to your API endpoint
    val appId: String,
    val userId: String? = null,
    val maxBatchSize: Int = 100,
    val flushIntervalSeconds: Int = 30,
    val retryDelayMs: Long = 2000,
    // interval (seconds) for re-checking server status when SDK has been disabled by server response 300
    val recheckIntervalSeconds: Int = 60
)
