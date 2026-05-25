package com.tracker.androidsdkclaude.model

data class Event(
    val name: String,
    val properties: Map<String, Any>,
    val timestamp: Long = System.currentTimeMillis(),
    val time: String = getCurrentIsoTime(),
    val occurredAt: String = getCurrentIsoTime()
){
    companion object {
        fun getCurrentIsoTime(): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            return sdf.format(java.util.Date())
        }
    }
}
