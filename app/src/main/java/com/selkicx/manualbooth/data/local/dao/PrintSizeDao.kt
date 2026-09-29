package com.selkicx.manualbooth.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.selkicx.manualbooth.data.local.entity.PrintSizeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrintSizeDao {
    @Query("SELECT * FROM print_sizes ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<PrintSizeEntity>>

    @Query("SELECT * FROM print_sizes WHERE id = :id")
    suspend fun getById(id: Long): PrintSizeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(printSize: PrintSizeEntity): Long

    @Delete
    suspend fun delete(printSize: PrintSizeEntity)
}
