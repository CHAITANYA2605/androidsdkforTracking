package com.tracker.pushsdk.modles

data class DeviceInfo(
    val platform: String = "Android",
    val osVersion: String,
    val model: String,
    val manufacturer: String,
    val brand: String,
    val sdkVersion: Int,
    val isWifi: Boolean,
    val simCompany: String
)