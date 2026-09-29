package com.selkicx.manualbooth.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Purely organizational grouping of templates (spec section 7). */
@Entity(tableName = "template_folders")
data class TemplateFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
