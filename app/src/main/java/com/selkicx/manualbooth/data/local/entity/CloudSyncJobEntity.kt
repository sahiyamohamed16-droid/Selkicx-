package com.selkicx.manualbooth.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.selkicx.manualbooth.domain.model.CloudSyncStatus

/** Tracked by WorkManager; retried automatically when connectivity returns (spec rule #31). */
@Entity(
    tableName = "cloud_sync_jobs",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class CloudSyncJobEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val status: CloudSyncStatus = CloudSyncStatus.PENDING,
    val attemptCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val lastError: String? = null
)
