package com.selkicx.manualbooth.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A configurable print size (spec section 34). Sizes come from the
 * database, not hardcoded values, so operators/admins can add new ones.
 */
@Entity(tableName = "print_sizes")
data class PrintSizeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,           // e.g. "A4", "4x6", "Photocard"
    val widthMm: Double,
    val heightMm: Double,
    val dpi: Int = 300,
    val isCustom: Boolean = false,
    val sortOrder: Int = 0
) {
    /** Output pixel dimensions at this size's configured DPI. */
    val widthPx: Int get() = mmToPx(widthMm, dpi)
    val heightPx: Int get() = mmToPx(heightMm, dpi)

    private fun mmToPx(mm: Double, dpi: Int): Int =
        ((mm / 25.4) * dpi).toInt()
}
