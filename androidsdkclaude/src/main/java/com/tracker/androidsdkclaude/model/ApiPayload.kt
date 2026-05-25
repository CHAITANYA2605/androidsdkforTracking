package com.tracker.androidsdkclaude.model

data class ApiPayload(
    val userId: String?,
    val deviceId: String,
    val deviceInfo: DeviceInfo,
    val events: List<Event>
)
