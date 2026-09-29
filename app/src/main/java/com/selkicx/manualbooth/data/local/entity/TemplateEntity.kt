package com.selkicx.manualbooth.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A template's photo mode is NEVER stored here (spec rule #4) - photo
 * mode belongs to the session. requiredPhotoCount is derived from the
 * number of PhotoHolderEntity rows for this template, not entered manually.
 */
@Entity(
    tableName = "templates",
    foreignKeys = [
        ForeignKey(
            entity = TemplateFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = PrintSizeEntity::class,
            parentColumns = ["id"],
            childColumns = ["printSizeId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("folderId"), Index("printSizeId")]
)
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val folderId: Long,
    val printSizeId: Long,
    val name: String,
    val orientation: String = "PORTRAIT", // PORTRAIT | LANDSCAPE
    val artworkPath: String,   // JPG/PNG, via Storage Access Framework
    val thumbnailPath: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
