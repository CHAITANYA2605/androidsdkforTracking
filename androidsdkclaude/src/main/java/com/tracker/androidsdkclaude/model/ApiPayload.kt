package com.tracker.androidsdkclaude.model

data class ApiPayload(
    val userid: String?,
    val deviceid: String,
    val deviceinfo: DeviceInfo,
    val events: List<Event>
)