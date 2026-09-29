package com.selkicx.manualbooth.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.selkicx.manualbooth.data.local.entity.CloudSyncJobEntity
import com.selkicx.manualbooth.data.local.entity.ShareLinkEntity
import com.selkicx.manualbooth.domain.model.CloudSyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudSyncJobDao {
    /** Never silently deleted - WorkManager retries pending/failed jobs (spec rule #31). */
    @Query("SELECT * FROM cloud_sync_jobs WHERE status IN (:statuses)")
    suspend fun getByStatuses(
        statuses: List<CloudSyncStatus> = listOf(CloudSyncStatus.PENDING, CloudSyncStatus.FAILED)
    ): List<CloudSyncJobEntity>

    @Query("SELECT * FROM cloud_sync_jobs WHERE sessionId = :sessionId")
    fun observeForSession(sessionId: Long): Flow<CloudSyncJobEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(job: CloudSyncJobEntity): Long
}

@Dao
interface ShareLinkDao {
    @Query("SELECT * FROM share_links WHERE sessionId = :sessionId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestForSession(sessionId: Long): ShareLinkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: ShareLinkEntity): Long
}
