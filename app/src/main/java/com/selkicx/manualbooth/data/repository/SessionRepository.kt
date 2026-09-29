package com.selkicx.manualbooth.data.repository

import com.selkicx.manualbooth.data.local.dao.FinalOutputDao
import com.selkicx.manualbooth.data.local.dao.SessionDao
import com.selkicx.manualbooth.data.local.dao.SessionPhotoDao
import com.selkicx.manualbooth.data.local.entity.SessionEntity
import com.selkicx.manualbooth.data.local.entity.SessionPhotoEntity
import com.selkicx.manualbooth.domain.adapters.CameraAdapter
import com.selkicx.manualbooth.domain.model.PhotoMode
import com.selkicx.manualbooth.domain.model.PrintStatus
import com.selkicx.manualbooth.domain.model.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Owns session lifecycle plus the live incoming-photo pipeline (spec
 * sections 14-17, 41): every camera photo is written to
 * [SessionPhotoEntity] for whichever session is currently active, with
 * thumbnail-first / full-res-later handling.
 *
 * The capture pipeline deliberately runs in a repository-owned scope
 * rather than a caller-supplied (e.g. ViewModel) scope: it must keep
 * receiving photos across screen navigation - e.g. from the New Session
 * wizard into the Active Session screen - and must not be cancelled just
 * because the screen that started the session was left.
 */
class SessionRepository(
    private val sessionDao: SessionDao,
    private val sessionPhotoDao: SessionPhotoDao,
    private val finalOutputDao: FinalOutputDao,
    private val cameraAdapter: CameraAdapter
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var captureCollectionJob: Job? = null

    fun observeSession(sessionId: Long): Flow<SessionEntity?> = sessionDao.observeById(sessionId)

    fun observeSessionPhotos(sessionId: Long): Flow<List<SessionPhotoEntity>> =
        sessionPhotoDao.observeBySession(sessionId)

    fun observeHistory(): Flow<List<SessionEntity>> = sessionDao.observeHistory()

    /** Reprint (spec section 49) reuses this rather than re-rendering. */
    suspend fun getFinalOutput(sessionId: Long) = finalOutputDao.getLatestForSession(sessionId)

    suspend fun generateNextDisplayNumber(): String {
        val count = sessionDao.countAll()
        return "#%04d".format(count + 1)
    }

    /**
     * Creates a new session and (re)binds the camera's incoming-photo
     * stream to it. Any previous binding is cancelled first so a late
     * photo can never attach to the wrong customer (spec section 41).
     */
    suspend fun startSession(
        displayNumber: String,
        templateId: Long,
        printSizeId: Long,
        photoMode: PhotoMode
    ): Long {
        captureCollectionJob?.cancel()

        val sessionId = sessionDao.upsert(
            SessionEntity(
                displayNumber = displayNumber,
                accountId = null,
                deviceId = null,
                templateId = templateId,
                printSizeId = printSizeId,
                photoMode = photoMode,
                state = SessionState.CAPTURING
            )
        )

        captureCollectionJob = cameraAdapter.observeIncomingPhotos()
            .onEach { photo ->
                val photoId = sessionPhotoDao.upsert(
                    SessionPhotoEntity(
                        sessionId = sessionId,
                        cameraFilename = photo.cameraFilename,
                        thumbnailPath = photo.thumbnailPath,
                        fullResPath = photo.fullResPath,
                        captureTimestamp = photo.captureTimestamp,
                        receiveTimestamp = photo.receiveTimestamp,
                        isFullResReady = photo.fullResPath != null
                    )
                )
                if (photo.fullResPath == null) {
                    repositoryScope.launch {
                        val path = cameraAdapter.fetchFullResolution(photo.cameraFilename)
                        sessionPhotoDao.markFullResReady(photoId, path)
                    }
                }
            }
            .launchIn(repositoryScope)

        // Start watching Android's image library only after the collector is
        // attached, so a fast Camera Connect/NFC delivery cannot be dropped.
        cameraAdapter.beginSession(sessionId)

        return sessionId
    }

    /** Persists the operator's ordered photo selection ahead of rendering. */
    suspend fun saveSelection(sessionId: Long, orderedSessionPhotoIds: List<Long>) {
        val session = sessionDao.getById(sessionId) ?: return
        sessionDao.upsert(
            session.copy(
                selectedPhotoIdsOrdered = orderedSessionPhotoIds.joinToString(","),
                state = SessionState.READY_TO_PRINT
            )
        )
    }

    /**
     * Closes the current session and starts a fresh one for the next
     * customer, retaining the same template/size/photo mode (spec
     * section 22). Returns -1 if the closing session can't be found.
     */
    suspend fun nextCustomer(closingSessionId: Long): Long {
        val closing = sessionDao.getById(closingSessionId) ?: return -1
        sessionDao.upsert(
            closing.copy(state = SessionState.COMPLETED, endTime = System.currentTimeMillis())
        )
        return startSession(
            displayNumber = generateNextDisplayNumber(),
            templateId = closing.templateId,
            printSizeId = closing.printSizeId,
            photoMode = closing.photoMode
        )
    }

    suspend fun updateState(sessionId: Long, state: SessionState) =
        sessionDao.updateState(sessionId, state)

    /** Keeps the lifecycle and printable status columns consistent. */
    suspend fun updatePrintProgress(
        sessionId: Long,
        state: SessionState,
        printStatus: PrintStatus
    ) {
        val session = sessionDao.getById(sessionId) ?: return
        sessionDao.upsert(session.copy(state = state, printStatus = printStatus))
    }
}
