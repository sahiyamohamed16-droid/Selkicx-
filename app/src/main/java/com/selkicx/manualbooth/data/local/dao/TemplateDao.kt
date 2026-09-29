package com.selkicx.manualbooth.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.selkicx.manualbooth.data.local.entity.PhotoHolderEntity
import com.selkicx.manualbooth.data.local.entity.TemplateEntity
import com.selkicx.manualbooth.data.local.entity.TemplateFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateFolderDao {
    @Query("SELECT * FROM template_folders ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<TemplateFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(folder: TemplateFolderEntity): Long

    @Delete
    suspend fun delete(folder: TemplateFolderEntity)
}

@Dao
interface TemplateDao {
    /** Only templates compatible with the chosen print size (spec section 7). */
    @Query("SELECT * FROM templates WHERE folderId = :folderId AND printSizeId = :printSizeId")
    fun observeByFolderAndSize(folderId: Long, printSizeId: Long): Flow<List<TemplateEntity>>

    /** All templates in a folder, any size - used by the Admin template list (spec section 24). */
    @Query("SELECT * FROM templates WHERE folderId = :folderId")
    fun observeByFolder(folderId: Long): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE printSizeId = :printSizeId")
    fun observeBySize(printSizeId: Long): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getById(id: Long): TemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: TemplateEntity): Long

    @Delete
    suspend fun delete(template: TemplateEntity)
}

@Dao
interface PhotoHolderDao {
    @Query("SELECT * FROM photo_holders WHERE templateId = :templateId ORDER BY slotNumber ASC")
    fun observeByTemplate(templateId: Long): Flow<List<PhotoHolderEntity>>

    /** Required photo count = holder count for the template (spec rule #13). */
    @Query("SELECT COUNT(*) FROM photo_holders WHERE templateId = :templateId")
    suspend fun countForTemplate(templateId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(holder: PhotoHolderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(holders: List<PhotoHolderEntity>)

    @Delete
    suspend fun delete(holder: PhotoHolderEntity)

    /** Used by the holder editor when removing a holder that only exists as an id (spec section 27). */
    @Query("DELETE FROM photo_holders WHERE id = :id")
    suspend fun deleteById(id: Long)
}
