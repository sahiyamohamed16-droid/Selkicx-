package com.selkicx.manualbooth.cloud

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val PERIODIC_WORK_NAME = "cloud_sync_periodic"
private const val ONE_TIME_WORK_NAME = "cloud_sync_now"

/**
 * Schedules [CloudSyncWorker] (spec sections 44-46). A periodic request
 * keeps retrying whenever connectivity is available (WorkManager itself
 * defers the work until the network constraint is met, so this covers
 * "must work fully offline, sync resumes when connection returns" - spec
 * rule #31 - without any manual connectivity-listening code); a one-time
 * request is also enqueued right after a session's Final Output is
 * rendered, so a newly completed session doesn't wait for the next
 * periodic window.
 */
class CloudSyncScheduler(private val context: Context) {

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<CloudSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun syncNow() {
        val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }
}
