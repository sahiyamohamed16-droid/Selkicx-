package com.selkicx.manualbooth.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.selkicx.manualbooth.data.local.entity.PrinterProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrinterProfileDao {
    @Query("SELECT * FROM printer_profiles WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<PrinterProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: PrinterProfileEntity): Long
}
