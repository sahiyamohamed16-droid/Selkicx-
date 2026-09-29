package com.selkicx.manualbooth.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Configured once in Admin, not re-asked per customer (spec section 37). */
@Entity(tableName = "printer_profiles")
data class PrinterProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val connectionType: String,   // WIFI | USB_OTG | ANDROID_PRINT_SERVICE | MANUFACTURER_API
    val defaultPrintSizeId: Long?,
    val borderless: Boolean = false,
    val printQuality: String = "HIGH",
    val orientation: String = "PORTRAIT",
    val copies: Int = 1,
    val isActive: Boolean = true
)
