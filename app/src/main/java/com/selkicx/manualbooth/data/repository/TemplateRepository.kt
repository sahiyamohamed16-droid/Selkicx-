package com.selkicx.manualbooth.data.repository

import com.selkicx.manualbooth.data.local.dao.PhotoHolderDao
import com.selkicx.manualbooth.data.local.dao.PrintSizeDao
import com.selkicx.manualbooth.data.local.dao.TemplateDao
import com.selkicx.manualbooth.data.local.dao.TemplateFolderDao
import com.selkicx.manualbooth.data.local.entity.PhotoHolderEntity
import com.selkicx.manualbooth.data.local.entity.PrintSizeEntity
import com.selkicx.manualbooth.data.local.entity.TemplateEntity
import com.selkicx.manualbooth.data.local.entity.TemplateFolderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Both the read side of the New Session flow (spec sections 6-13) and
 * the write side of Admin > Templates (spec sections 24-28): folder
 * create/rename/delete, template creation, and holder placement. Photo
 * mode is never stored here - it belongs to the session, never the
 * template (spec rule #4). Required photo count is always derived from
 * holder count, never stored/entered manually (spec rule #13).
 */
class TemplateRepository(
    private val printSizeDao: PrintSizeDao,
    private val folderDao: TemplateFolderDao,
    private val templateDao: TemplateDao,
    private val photoHolderDao: PhotoHolderDao
) {
    // ---- Read side (New Session flow + general lookups) -----------------

    /**
     * Makes a fresh installation usable without a debug-only database script.
     * Admin UI can add full Print Size CRUD later; these production defaults
     * are inserted only when the table is empty and never overwrite operator
     * configuration.
     */
    suspend fun seedDefaultPrintSizesIfEmpty() {
        if (printSizeDao.observeAll().first().isNotEmpty()) return

        listOf(
            PrintSizeEntity(
                name = "A4",
                widthMm = 210.0,
                heightMm = 297.0,
                dpi = 300,
                sortOrder = 0
            ),
            PrintSizeEntity(
                name = "4x6",
                widthMm = 101.6,
                heightMm = 152.4,
                dpi = 300,
                sortOrder = 1
            ),
            PrintSizeEntity(
                name = "Photocard",
                widthMm = 54.0,
                heightMm = 86.0,
                dpi = 300,
                sortOrder = 2
            )
        ).forEach { printSizeDao.upsert(it) }
    }

    fun observePrintSizes(): Flow<List<PrintSizeEntity>> = printSizeDao.observeAll()

    suspend fun getPrintSize(id: Long): PrintSizeEntity? = printSizeDao.getById(id)

    fun observeFolders(): Flow<List<TemplateFolderEntity>> = folderDao.observeAll()

    /** Templates compatible with the chosen print size, inside the chosen folder. */
    fun observeTemplates(folderId: Long, printSizeId: Long): Flow<List<TemplateEntity>> =
        templateDao.observeByFolderAndSize(folderId, printSizeId)

    /** All templates in a folder regardless of size - used by the Admin template list. */
    fun observeTemplatesInFolder(folderId: Long): Flow<List<TemplateEntity>> =
        templateDao.observeByFolder(folderId)

    fun observeHolders(templateId: Long): Flow<List<PhotoHolderEntity>> =
        photoHolderDao.observeByTemplate(templateId)

    suspend fun getHoldersOnce(templateId: Long): List<PhotoHolderEntity> =
        photoHolderDao.observeByTemplate(templateId).first()

    /** Required photo count = holder count for this template (spec rule #13). */
    suspend fun requiredPhotoCount(templateId: Long): Int =
        photoHolderDao.countForTemplate(templateId)

    suspend fun getTemplate(templateId: Long): TemplateEntity? = templateDao.getById(templateId)

    // ---- Write side (Admin > Templates) --------------------------------

    suspend fun createFolder(name: String): Long = folderDao.upsert(TemplateFolderEntity(name = name))

    suspend fun renameFolder(folder: TemplateFolderEntity, newName: String) {
        folderDao.upsert(folder.copy(name = newName))
    }

    suspend fun deleteFolder(folder: TemplateFolderEntity) = folderDao.delete(folder)

    suspend fun createTemplate(
        folderId: Long,
        printSizeId: Long,
        name: String,
        artworkPath: String,
        thumbnailPath: String?
    ): Long = templateDao.upsert(
        TemplateEntity(
            folderId = folderId,
            printSizeId = printSizeId,
            name = name,
            artworkPath = artworkPath,
            thumbnailPath = thumbnailPath
        )
    )

    suspend fun upsertHolder(holder: PhotoHolderEntity): Long = photoHolderDao.upsert(holder)

    suspend fun deleteHolderById(id: Long) = photoHolderDao.deleteById(id)
}
