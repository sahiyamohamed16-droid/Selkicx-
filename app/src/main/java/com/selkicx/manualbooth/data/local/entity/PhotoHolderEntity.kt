package com.selkicx.manualbooth.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.selkicx.manualbooth.domain.model.CropMode
import com.selkicx.manualbooth.domain.model.PhotoHolderShape

/**
 * A photo mask placed over a template. Coordinates are normalized
 * (0.0-1.0) so they are independent of phone display resolution
 * (spec section 28). slotNumber determines holder ordering / the
 * required-photo-count and photo-assignment order (spec rule #14).
 */
@Entity(
    tableName = "photo_holders",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("templateId")]
)
data class PhotoHolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val templateId: Long,
    val slotNumber: Int,
    val shape: PhotoHolderShape,
    val x: Float,       // normalized 0f..1f
    val y: Float,
    val width: Float,
    val height: Float,
    val cropMode: CropMode = CropMode.CENTER_CROP
)
