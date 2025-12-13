package com.tracker.androidsdkclaude.model

data class Event(
    val name: String,
    val properties: Map<String, Any>,
    val timestamp: Long = System.currentTimeMillis()
)