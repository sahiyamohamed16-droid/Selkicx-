package com.selkicx.manualbooth.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Belongs to the CLOUD session, not the phone (spec rule #30). Uses a
 * secure random token - never a sequential public URL (spec section 46).
 */
@Entity(
    tableName = "share_links",
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
data class ShareLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val token: String,             // secure random, e.g. UUID or SecureRandom-derived
    val createdAt: Long = System.currentTimeMillis(),
    val createdFrom: String = "DEVICE" // DEVICE | WEBSITE
)
