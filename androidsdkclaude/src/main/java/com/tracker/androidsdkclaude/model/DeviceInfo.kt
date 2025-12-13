package com.tracker.androidsdkclaude.model

data class DeviceInfo(
    val platform: String = "Android",
    val os_version: String,
    val model: String,
    val manufacturer: String,
    val brand: String,
    val sdk_version: Int,
    val iswifi: Boolean,
    val Simcompany : String,
)