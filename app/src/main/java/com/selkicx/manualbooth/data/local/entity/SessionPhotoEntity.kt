package com.selkicx.manualbooth.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One captured original photograph belonging to a session. Every
 * captured photo is stored here regardless of whether it was selected
 * for printing (spec rule #22). Timestamps help avoid a delayed camera
 * transfer attaching to the wrong customer (spec section 41).
 */
@Entity(
    tableName = "session_photos",
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
data class SessionPhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val cameraFilename: String,
    val thumbnailPath: String?,
    val fullResPath: String?,           // null until background download completes
    val captureTimestamp: Long,
    val receiveTimestamp: Long,
    val isFullResReady: Boolean = false
)
