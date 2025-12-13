package com.tracker.androidsdk

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class RetryWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val events = Storage.fetch()
        val success = ApiService.send(events)
        if (success) Storage.clear()
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<RetryWorker>(15, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("sdk_retry", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
