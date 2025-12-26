package com.tracker.androidsdkclaude

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import com.tracker.androidsdkclaude.AppLifecycleTracker
import android.database.Cursor
import android.net.Uri

class TrackerInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        context?.let { appContext ->
            // Access Application and register lifecycle listener
            (appContext as? android.app.Application)?.registerActivityLifecycleCallbacks(
                AppLifecycleTracker()
            )
        }
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
