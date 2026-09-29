package com.selkicx.manualbooth.rendering

import android.content.Context
import com.selkicx.manualbooth.cloud.CloudSyncScheduler
import com.selkicx.manualbooth.data.local.dao.CloudSyncJobDao
import com.selkicx.manualbooth.data.local.dao.FinalOutputDao
import com.selkicx.manualbooth.data.local.dao.PhotoHolderDao
import com.selkicx.manualbooth.data.local.dao.PrintSizeDao
import com.selkicx.manualbooth.data.local.dao.SessionDao
import com.selkicx.manualbooth.data.local.dao.SessionPhotoDao
import com.selkicx.manualbooth.data.local.dao.TemplateDao
import com.selkicx.manualbooth.data.local.entity.CloudSyncJobEntity
import com.selkicx.manualbooth.data.local.entity.FinalOutputEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Orchestrates Milestone 4: loads everything [FinalOutputRenderer] needs
 * for a session, renders the Final Output, persists it, and links it back
 * onto the session (spec sections 32-35). This is the single seam
 * `ActiveSessionViewModel` calls on PRINT confirmation.
 *
 * It also enqueues the session's cloud sync job here (Milestone 8, spec
 * sections 44-46): once a Final Output exists, the session's full
 * package - all captured originals plus this file - is ready to upload,
 * independent of whether the operator has moved on to Next Customer yet.
 */
class FinalOutputUseCase(
    private val context: Context,
    private val sessionDao: SessionDao,
    private val sessionPhotoDao: SessionPhotoDao,
    private val templateDao: TemplateDao,
    private val photoHolderDao: PhotoHolderDao,
    private val printSizeDao: PrintSizeDao,
    private val finalOutputDao: FinalOutputDao,
    private val cloudSyncJobDao: CloudSyncJobDao,
    private val cloudSyncScheduler: CloudSyncScheduler,
    private val renderer: FinalOutputRenderer = FinalOutputRenderer()
) {
    /**
     * @param orderedSessionPhotoIds the operator's selection, in order;
     *   order determines holder assignment (spec rule #14).
     */
    suspend fun render(sessionId: Long, orderedSessionPhotoIds: List<Long>): File =
        withContext(Dispatchers.IO) {
            val session = sessionDao.getById(sessionId)
                ?: error("Session $sessionId not found")
            val template = templateDao.getById(session.templateId)
                ?: error("Template ${session.templateId} not found")
            val printSize = printSizeDao.getById(session.printSizeId)
                ?: error("Print size ${session.printSizeId} not found")
            val holders = photoHolderDao.observeByTemplate(template.id).first()
            val sessionPhotos = sessionPhotoDao.observeBySession(sessionId).first()
            val photosById = sessionPhotos.associateBy { it.id }

            // Original photo files are only ever read here, never modified (spec rule #15).
            val orderedPaths = orderedSessionPhotoIds.map { id ->
                val photo = photosById[id] ?: error("Session photo $id not found")
                photo.fullResPath ?: photo.thumbnailPath
                ?: error("No usable file for session photo $id")
            }

            val outputDir = File(context.filesDir, "sessions/${session.id}/final_output")
            val outputFile = File(outputDir, "final_${System.currentTimeMillis()}.jpg")

            val rendered = renderer.render(
                template = template,
                holders = holders,
                orderedPhotoPaths = orderedPaths,
                photoMode = session.photoMode,
                outputWidthPx = printSize.widthPx,
                outputHeightPx = printSize.heightPx,
                outputFile = outputFile
            )

            val finalOutputId = finalOutputDao.upsert(
                FinalOutputEntity(
                    sessionId = session.id,
                    templateId = template.id,
                    photoMode = session.photoMode,
                    filePath = rendered.absolutePath,
                    widthPx = printSize.widthPx,
                    heightPx = printSize.heightPx
                )
            )
            sessionDao.upsert(session.copy(finalOutputId = finalOutputId))

            cloudSyncJobDao.upsert(CloudSyncJobEntity(sessionId = session.id))
            cloudSyncScheduler.syncNow()

            rendered
        }
}
