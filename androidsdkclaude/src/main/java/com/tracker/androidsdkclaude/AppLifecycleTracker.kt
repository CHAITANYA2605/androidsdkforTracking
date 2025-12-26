package com.tracker.androidsdkclaude

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log

class AppLifecycleTracker : Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        safeTrack("app_open")
    }

    override fun onActivityResumed(activity: Activity) {
        safeTrack("app_foreground")
    }

    override fun onActivityPaused(activity: Activity) {
        safeTrack("app_background")
    }

    override fun onActivityDestroyed(activity: Activity) {
        safeTrack("app_closed")
    }

    private fun safeTrack(event: String) {
        try {
            EventTracker.getInstance().track(event)
        } catch (e: IllegalStateException) {
            // NOT initialized yet → ignore without crashing
            Log.w("EventTracker", "SDK not initialized yet, delaying event: $event")
        }
    }

    override fun onActivityStarted(a: Activity) {}
    override fun onActivityStopped(a: Activity) {}
    override fun onActivitySaveInstanceState(a: Activity, out: Bundle) {}
}