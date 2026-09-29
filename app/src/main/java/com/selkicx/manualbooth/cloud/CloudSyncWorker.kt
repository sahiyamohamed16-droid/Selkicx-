package com.selkicx.manualbooth.cloud

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.selkicx.manualbooth.data.local.AppDatabase
import com.selkicx.manualbooth.domain.model.CloudSyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background sync stub (spec sections 44-46): retries PENDING/FAILED
 * CloudSyncJob rows, tracked independently of QR sharing (spec rule
 * #28). The actual SelkicX upload API is a separate backend service
 * (spec section 46) out of scope for this client scaffold - this
 * worker's job is the retry/status contract around it: never silently
 * drop an unsynced session (spec rule #31), and keep retrying once
 * connectivity returns (enforced by this work request's network
 * constraint, set in [CloudSyncScheduler]).
 *
 * WorkManager instantiates this via its default factory, which only
 * supplies (Context, WorkerParameters) - so it reaches the database
 * through [AppDatabase.getInstance] rather than the app's hand-rolled
 * [com.selkicx.manualbooth.di.AppContainer].
 */
class CloudSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(applicationContext)
        val jobDao = database.cloudSyncJobDao()
        val sessionDao = database.sessionDao()

        val pendingJobs = jobDao.getByStatuses()
        var anyFailed = false

        pendingJobs.forEach { job ->
            jobDao.upsert(job.copy(status = CloudSyncStatus.SYNCING, attemptCount = job.attemptCount + 1))

            val uploaded = uploadSessionStub(job.sessionId)

            if (uploaded) {
                jobDao.upsert(job.copy(status = CloudSyncStatus.SYNCED, lastAttemptAt = System.currentTimeMillis()))
                sessionDao.getById(job.sessionId)?.let { session ->
                    sessionDao.upsert(session.copy(cloudSyncStatus = CloudSyncStatus.SYNCED))
                }
            } else {
                anyFailed = true
                jobDao.upsert(
                    job.copy(
                        status = CloudSyncStatus.FAILED,
                        lastAttemptAt = System.currentTimeMillis(),
                        lastError = "SelkicX cloud upload not yet implemented (client-side stub)"
                    )
                )
            }
        }

        if (anyFailed) Result.retry() else Result.success()
    }

    /**
     * TODO(real backend): replace with a Retrofit/OkHttp call uploading
     * this session's originals + Final Output to SelkicX cloud storage
     * (spec section 44). Always returns false for now so jobs stay
     * PENDING/FAILED and keep retrying rather than being silently
     * marked done - status visible in Session History as "Pending"
     * until this is wired to a real endpoint.
     */
    private fun uploadSessionStub(sessionId: Long): Boolean = false
}
