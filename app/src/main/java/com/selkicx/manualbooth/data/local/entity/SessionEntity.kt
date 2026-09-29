package com.selkicx.manualbooth.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.selkicx.manualbooth.domain.model.CloudSyncStatus
import com.selkicx.manualbooth.domain.model.PhotoMode
import com.selkicx.manualbooth.domain.model.PrintStatus
import com.selkicx.manualbooth.domain.model.SessionState

/**
 * One isolated customer session (spec sections 40-41). Photo mode is
 * stored HERE, never on the template. selectedPhotoIdsOrdered preserves
 * selection order, which determines holder assignment (spec rule #14).
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val displayNumber: String,          // e.g. "#0048"
    val accountId: Long?,
    val deviceId: String?,
    val templateId: Long,
    val printSizeId: Long,
    val photoMode: PhotoMode,
    val state: SessionState = SessionState.CREATED,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val capturedPhotoCount: Int = 0,
    /** Ordered, comma-separated SessionPhoto ids; order = holder assignment. */
    val selectedPhotoIdsOrdered: String = "",
    val printStatus: PrintStatus = PrintStatus.NOT_PRINTED,
    val cloudSyncStatus: CloudSyncStatus = CloudSyncStatus.PENDING,
    val finalOutputId: Long? = null,
    val shareToken: String? = null
)
