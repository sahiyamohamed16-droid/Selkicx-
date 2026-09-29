package com.selkicx.manualbooth.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.selkicx.manualbooth.domain.model.PhotoMode

/**
 * The rendered Final Output image (spec section 32): template +
 * selected photos + session photo mode + holder masks. This is what
 * gets printed, saved, uploaded, shared via QR, and reused on reprint.
 */
@Entity(
    tableName = "final_outputs",
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
data class FinalOutputEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val templateId: Long,
    val photoMode: PhotoMode,
    val filePath: String,
    val widthPx: Int,
    val heightPx: Int,
    val renderedAt: Long = System.currentTimeMillis()
)
