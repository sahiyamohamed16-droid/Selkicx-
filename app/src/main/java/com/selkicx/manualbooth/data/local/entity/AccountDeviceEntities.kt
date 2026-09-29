package com.selkicx.manualbooth.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The photographer's SelkicX cloud account. */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: Long,
    val email: String,
    val displayName: String?
)

/** This physical phone/tablet running the booth app. */
@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey
    val id: String,           // stable device UUID
    val accountId: Long?,
    val label: String?,
    val lastSeenAt: Long = System.currentTimeMillis()
)
